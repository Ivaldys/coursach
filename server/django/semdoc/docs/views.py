from django.shortcuts import render
from rest_framework.views import APIView
from django.http import JsonResponse
from . import models
import json
import boto3
import uuid
from django.contrib.auth.hashers import make_password
from django.core.mail import send_mail
from django.conf import settings
from rest_framework import status
from rest_framework.response import Response
from rest_framework.authtoken.models import Token
from django.contrib.auth import authenticate
from django.contrib.auth.hashers import check_password
from rest_framework.permissions import IsAuthenticated
from rest_framework.authentication import TokenAuthentication
from django.utils import timezone
import random
from django.shortcuts import get_object_or_404
from PyPDF2 import PdfReader
import pytesseract
from pdf2image import convert_from_bytes
from translate import Translator
from io import BytesIO
import fitz

# Create your views here.
def index(request):
    return Response("Страница приложения docs")

def katyaa(request):
    return Response("<h1>Катя жопкевич тот еще</h1>")

class RegisterView(APIView):
    def post(self, request):
        email = request.data.get('email')
        name  = request.data.get('username')
        password = request.data.get('password')

        if not email or not name  or not password:
            return Response({'error': 'Не все поля заполнены'}, status=status.HTTP_400_BAD_REQUEST)

        if models.MyUser.objects.filter(email=email).exists():
            return Response({'error': 'Пользователь с такой почтой уже существует'}, status=status.HTTP_400_BAD_REQUEST)

        if len(password) < 8:
            return Response({'error': 'Пароль слишком короткий'}, status=status.HTTP_400_BAD_REQUEST)

        user = models.MyUser.objects.create_user(email=email, name=name , password=password)
        user.email_confirmed = False
        user.generate_confirmation_code()
        user.save()
        send_mail(
            'Код-подтверждения',
            f'Код подтверждения : {user.confirmation_code}',
            from_email=settings.DEFAULT_FROM_EMAIL,
            recipient_list=[email],
            fail_silently=False,
        )
        return Response({'message': 'Регистрация успешна, проверьте почту'}, status=status.HTTP_201_CREATED)

class ConfirmEmailView(APIView):
    def post(self, request):
        email = request.data.get('email')
        code = request.data.get('code')

        if not email or not code:
            return Response({'error': 'Не все поля заполнены'}, status=status.HTTP_400_BAD_REQUEST)
        try:
            user = models.MyUser.objects.get(email=email)
        except (user.DoesNotExist):
            return Response({'error': 'Пользователь не найден'}, status=status.HTTP_404_NOT_FOUND)

        if user.is_code_expired():
             return Response({'error': 'Код подтверждения устарел'}, status=status.HTTP_400_BAD_REQUEST)

        if user.confirmation_code != code:
            return Response({'error': 'Неверный код подтверждения'}, status=status.HTTP_400_BAD_REQUEST)

        user.email_confirmed = True
        user.confirmation_code = ''
        user.save()

        return Response({'message': 'Аккаунт подтверждён'}, status=status.HTTP_200_OK)


class LoginView(APIView):
    def post(self, request):
        email = request.data.get('email')
        password = request.data.get('password')
        if not email or not password:
            return Response({'error': 'Не все поля заполнены'}, status=status.HTTP_400_BAD_REQUEST)

        try:
            user = models.MyUser.objects.get(email=email)
        except user.DoesNotExist:
            return Response({'error': 'Пользователь не найден'}, status=status.HTTP_404_NOT_FOUND)

        if not user.check_password(password):
            return Response({'error': 'Неверный пароль'}, status=status.HTTP_401_UNAUTHORIZED)

        if not user.email_confirmed:
            user.generate_confirmation_code()
            user.save()
            send_mail(
                'Код-подтверждения',
                f'Код подтверждения : {user.confirmation_code}',
                from_email=settings.DEFAULT_FROM_EMAIL,
                recipient_list=[email],
                fail_silently=False,
            )
            return Response({
                'error': 'Почта не подтверждена',
                'email': email,
                'message': 'Код подтверждения отправлен на почту'
            }, status=status.HTTP_403_FORBIDDEN)

        token, _ = Token.objects.get_or_create(user=user)

        return Response({'token': token.key, 'message': 'Авторизация успешна'}, status=status.HTTP_200_OK)

class CreateFamilyView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]
    def post(self, request):
        user = request.user
        name = request.data.get('name')
        password = request.data.get('password')
        print(name, " ", password)

        if not name or not password:
            print(1)
            return Response({'error': 'Название и пароль обязательны'}, status=400)

        if len(password) < 8 or not password.isalnum():
            print(2)
            return Response({'error': 'Пароль должен быть не короче 8 символов и состоять из латиницы и цифр'}, status=400)


        family = models.Family(name=name)
        family.set_password(password)
        family.save()

        models.FamilyMembership.objects.create(user=user, family=family, role='owner')
        return Response({'message': 'Семья создана', 'family_id': family.id}, status=201)


class JoinFamilyView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        family_id = request.data.get('family_id')
        password = request.data.get('password')

        try:
            family = models.Family.objects.get(id=family_id)
        except models.Family.DoesNotExist:
            return Response({'error': 'Семья не найдена'}, status=404)

        if not check_password(password, family.join_password):
            return Response({'error': 'Неверный пароль'}, status=403)

        if models.FamilyJoinRequest.objects.filter(user=user, family=family, status='pending').exists():
            return Response({'error': 'Заявка уже отправлена'}, status=400)

        if models.FamilyMembership.objects.filter(user=user, family=family).exists():
            return Response({'error': 'Вы уже участник этой семьи'}, status=400)

        models.FamilyJoinRequest.objects.create(user=user, family=family)
        return Response({'message': 'Заявка отправлена'}, status=201)

class HandleJoinRequestView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        request_id = request.data.get('request_id')
        action = request.data.get('action')  # 'accept' или 'reject'

        try:
            join_request = models.FamilyJoinRequest.objects.get(id=request_id)
        except models.FamilyJoinRequest.DoesNotExist:
            return Response({'error': 'Заявка не найдена'}, status=404)
        # Проверяем, что текущий пользователь — создатель семьи
        try:
            membership = models.FamilyMembership.objects.get(user=user, family=join_request.family, role='owner')
        except models.FamilyMembership.DoesNotExist:
            return Response({'error': 'Вы не являетесь создателем этой семьи'}, status=403)

        if join_request.status != 'pending':
            return Response({'error': 'Заявка уже обработана'}, status=400)

        if action == 'accept':
            join_request.status = 'accepted'
            models.FamilyMembership.objects.create(user=join_request.user, family=join_request.family, role='member')
        elif action == 'reject':
            join_request.status = 'rejected'
        else:
            return Response({'error': 'Недопустимое действие'}, status=400)

        join_request.resolved_at = timezone.now()
        join_request.save()

        # TODO: здесь можно добавить уведомления пользователю о результате

        return Response({'message': f'Заявка {action}ed'}, status=200)

class ListJoinRequestsView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request, family_id):
        user = request.user

        try:
            membership = models.FamilyMembership.objects.get(user=user, family_id=family_id, role='owner')
        except models.FamilyMembership.DoesNotExist:
            return Response({'error': 'Вы не являетесь создателем этой семьи'}, status=403)

        requests = models.FamilyJoinRequest.objects.filter(family_id=family_id, status='pending')
        data = [
            {
                'id': r.id,
                'user_email': r.user.email,
                'user_name': r.user.name,
                'created_at': r.created_at
            }
            for r in requests
        ]
        return Response({'requests': data}, status=200)

class LeaveFamilyView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        family_id = request.data.get('family_id')

        try:
            membership = models.FamilyMembership.objects.get(user=user, family_id=family_id)
        except models.FamilyMembership.DoesNotExist:
            return Response({'error': 'Вы не состоите в этой семье'}, status=400)

        if membership.role == 'owner':
            return Response({'error': 'Создатель не может покинуть семью'}, status=403)

        membership.delete()
        return Response({'message': 'Вы покинули семью'}, status=200)

class UserFamiliesView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        memberships = models.FamilyMembership.objects.filter(user=user)
        data = [
            {
                'family_id': m.family.id,
                'family_name': m.family.name,
                'role': m.role,
            }
            for m in memberships
        ]
        return Response({'families': data}, status=200)

class FamilyDetailView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request, family_id):
        user = request.user
        try:
            membership = models.FamilyMembership.objects.get(user=user, family_id=family_id)
        except models.FamilyMembership.DoesNotExist:
            return Response({'error': 'Вы не состоите в этой семье'}, status=403)

        family = membership.family
        members = models.FamilyMembership.objects.filter(family=family)

        members_data = [
            {
                'user_id': m.user.id,
                'name': m.user.name,
                'email': m.user.email,
                'role': m.role
            }
            for m in members
        ]

        data = {
            'family_id': family.id,
            'family_name': family.name,
            'role': membership.role,
            'members': members_data
            # документы и папки добавим позже
        }
        return Response(data, status=200)


class TransferOwnershipView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request, family_id):
        user = request.user
        new_owner_id = request.data.get('new_owner_id')

        try:
            family = models.Family.objects.get(id=family_id)
            current_membership = models.FamilyMembership.objects.get(user=user, family=family)

            if current_membership.role != 'owner':
                return Response({'error': 'Только глава семьи может передать права'}, status=403)

            new_owner = models.MyUser.objects.get(id=new_owner_id)
            new_membership = models.FamilyMembership.objects.get(user=new_owner, family=family)

            current_membership.role = 'member'
            current_membership.save()

            new_membership.role = 'owner'
            new_membership.save()

            return Response({'message': 'Права переданы успешно'}, status=200)

        except Exception as e:
            return Response({'error': str(e)}, status=400)


class RemoveMemberView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request, family_id):
        user = request.user
        member_id = request.data.get('member_id')

        try:
            family = models.Family.objects.get(id=family_id)
            creator_membership = models.FamilyMembership.objects.get(user=user, family=family)

            if creator_membership.role != 'owner':
                return Response({'error': 'Только создатель семьи может исключать участников'}, status=403)

            member_to_remove = models.MyUser.objects.get(id=member_id)
            if member_to_remove == user:
                return Response({'error': 'Нельзя удалить самого себя'}, status=400)

            models.FamilyMembership.objects.filter(user=member_to_remove, family=family).delete()

            return Response({'message': 'Участник удален'}, status=200)

        except Exception as e:
            return Response({'error': str(e)}, status=400)

class ChangeFamilyPasswordView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request, family_id):
        user = request.user
        new_password = request.data.get('new_password')

        if not new_password or len(new_password) < 8 or not new_password.isalnum():
            return Response({'error': 'Пароль должен быть не короче 8 символов и состоять только из латинских букв и цифр'}, status=400)

        try:
            family = models.Family.objects.get(id=family_id)
            membership = models.FamilyMembership.objects.get(user=user, family=family)

            if membership.role != 'owner':
                return Response({'error': 'Только создатель семьи может изменить пароль'}, status=403)

            family.password = new_password
            family.save()

            return Response({'message': 'Пароль семьи успешно изменён'}, status=200)

        except Exception as e:
            return Response({'error': str(e)}, status=400)

class FamilyDetailView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request, family_id):
        user = request.user
        try:
            membership = models.FamilyMembership.objects.get(user=user, family_id=family_id)
        except models.FamilyMembership.DoesNotExist:
            return Response({'error': 'Вы не состоите в этой семье'}, status=403)

        family = membership.family

        data = {
            'family_id': family.id,
            'family_name': family.name,
            'role': membership.role,
            'join_password' : family.join_password
        }

        return Response(data, status=200)

class ChangeFamilyPasswordView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request, family_id):
        user = request.user
        new_password = request.data.get('new_password')

        if not new_password:
            return Response({'error': 'Новый пароль обязателен'}, status=400)

        if len(new_password) < 8 or not new_password.isalnum():
            return Response({'error': 'Пароль должен содержать минимум 8 символов и состоять из латиницы и цифр'}, status=400)

        try:
            membership = models.FamilyMembership.objects.get(user=user, family_id=family_id, role='owner')
        except models.FamilyMembership.DoesNotExist:
            return Response({'error': 'Вы не являетесь создателем этой семьи'}, status=403)

        family = membership.family
        family.set_password(new_password)
        family.save()

        return Response({'message': 'Пароль успешно изменён'}, status=200)

class UserProfileView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        email = user.email
        masked_email = email[0] + '***' + email[email.find('@') - 1:]  # Пример маскировки

        return Response({
            'name': user.name,
            'email': masked_email,
        })

class RequestEmailChangeView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]
    def post(self, request):
        user = request.user
        current_password = request.data.get('password')
        new_email = request.data.get('new_email')

        if not current_password or not new_email:
            return Response({'error': 'Текущий пароль и новый email обязательны'}, status=400)

        if not user.check_password(current_password):
            return Response({'error': 'Неверный пароль'}, status=403)

        # Генерируем код подтверждения
        code = f"{random.randint(1000, 9999)}"
        user.email_change_code = code
        user.new_email = new_email
        user.email_change_code_created_at = timezone.now()
        user.save()

        send_mail(
            'Код подтверждения смены email',
            f'Ваш код для подтверждения смены email: {code}',
            settings.DEFAULT_FROM_EMAIL,
            [new_email],
            fail_silently=False,
        )

        return Response({'message': 'Код подтверждения отправлен на новый email'})

class ConfirmEmailChangeView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        code = request.data.get('code')

        if not code:
            return Response({'error': 'Код подтверждения обязателен'}, status=400)

        if user.email_change_code != code:
            return Response({'error': 'Неверный код'}, status=400)


        user.email = user.new_email
        user.new_email = ''
        user.email_change_code = ''
        user.save()

        return Response({'message': 'Email успешно изменён'})

class RequestPasswordChangeView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user

        code = f"{random.randint(1000, 9999)}"
        user.password_change_code = code
        user.password_change_code_created_at = timezone.now()
        user.save()

        send_mail(
            'Код подтверждения смены пароля',
            f'Ваш код для смены пароля: {code}',
            settings.DEFAULT_FROM_EMAIL,
            [user.email],
            fail_silently=False,
        )

        return Response({'message': 'Код подтверждения отправлен на вашу почту'})

class ConfirmPasswordChangeView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        code = request.data.get('code')
        new_password = request.data.get('new_password')

        if not code or not new_password:
            return Response({'error': 'Все поля обязательны'}, status=400)

        if user.password_change_code != code:
            return Response({'error': 'Неверный код'}, status=400)

        user.set_password(new_password)
        user.password_change_code = ''
        user.save()

        return Response({'message': 'Пароль успешно изменён'})

class PasswordRecoveryRequestView(APIView):
    def post(self, request):
        email = request.data.get('email')
        try:
            user = models.MyUser.objects.get(email=email)
        except models.MyUser.DoesNotExist:
            return Response({'error': 'Пользователь не найден'}, status=404)

        code = f"{random.randint(1000, 9999)}"
        user.password_recovery_code = code
        user.password_recovery_code_created_at = timezone.now()
        user.save()

        send_mail(
            'Код для восстановления пароля',
            f'Ваш код восстановления: {code}',
            settings.DEFAULT_FROM_EMAIL,
            [email],
            fail_silently=False,
        )

        return Response({'message': 'Код для восстановления отправлен на почту'})

class PasswordRecoveryConfirmView(APIView):
    def post(self, request):
        email = request.data.get('email')
        code = request.data.get('code')
        new_password = request.data.get('new_password')
        confirm_password = request.data.get('confirm_password')

        if not email or not code or not new_password or not confirm_password:
            return Response({'error': 'Все поля обязательны'}, status=400)

        try:
            user = models.MyUser.objects.get(email=email)
        except models.MyUser.DoesNotExist:
            return Response({'error': 'Пользователь не найден'}, status=404)

        if user.password_recovery_code != code:
            return Response({'error': 'Неверный код'}, status=400)

        if new_password != confirm_password:
            return Response({'error': 'Пароли не совпадают'}, status=400)

        user.set_password(new_password)
        user.password_recovery_code = ''
        user.save()

        return Response({'message': 'Пароль успешно восстановлен'})

class EditNameView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        new_name = request.data.get('name')

        if not new_name:
            return Response({'error': 'Имя не может быть пустым'}, status=400)

        user.name = new_name
        user.save()

        return Response({'message': 'Имя успешно обновлено'})

from rest_framework.authtoken.models import Token

class LogoutView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        Token.objects.filter(user=user).delete()
        return Response({'message': 'Вы успешно вышли из аккаунта'})

class UploadPersonalDocumentView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user

        file_obj = request.FILES.get('file')
        if not file_obj:
            return Response({'error': 'Файл не предоставлен'}, status=400)

        name = request.data.get('name', '').strip()
        description = request.data.get('description', '').strip()
        folder_id = request.data.get('folder_id')

        # Проверка папки, если указана
        folder = None
        if folder_id:
            try:
                folder = models.PersonalFolder.objects.get(id=folder_id, owner=user)
            except models.PersonalFolder.DoesNotExist:
                return Response({'error': 'Папка не найдена'}, status=404)

        # Если имя не указано, подставляем "Новый документ" с индексом
        if not name:
            base_name = "Новый документ"
            existing_docs = models.PersonalDocument.objects.filter(owner=user, name__startswith=base_name).count()
            name = f"{base_name} ({existing_docs + 1})" if existing_docs else base_name
        elif len(name) > 255:
            return Response({'error': 'Имя документа слишком длинное (максимум 255 символов)'}, status=400)

        if description and len(description) > 500:
            return Response({'error': 'Описание слишком длинное (максимум 500 символов)'}, status=400)

        # Загрузка файла в S3
        file_extension = file_obj.name.split('.')[-1]
        s3_key = f'personal_docs/{user.id}/{uuid.uuid4()}.{file_extension}'

        s3 = boto3.client('s3',
                           aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                           aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
                           region_name=settings.AWS_S3_REGION_NAME)

        try:
            s3.upload_fileobj(file_obj, settings.AWS_STORAGE_BUCKET_NAME, s3_key)
        except Exception as e:
            return Response({'error': f'Ошибка загрузки файла: {str(e)}'}, status=500)

        # Создаем запись в базе
        doc = models.PersonalDocument.objects.create(
            owner=user,
            name=name,
            description=description,
            s3_key=s3_key,
            folder=folder
        )

        serializer = models.PersonalDocumentSerializer(doc)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class CreatePersonalFolderView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        name = request.data.get('name')
        parent_id = request.data.get('parent_id')

        if not name:
            return Response({'error': 'Название папки обязательно'}, status=400)

        parent_folder = None
        if parent_id:
            parent_folder = get_object_or_404(models.PersonalFolder, id=parent_id, owner=request.user)

        folder = models.PersonalFolder.objects.create(
            owner=request.user,
            name=name,
            parent=parent_folder
        )

        return Response({
            'id': folder.id,
            'name': folder.name,
            'created_at': folder.created_at,
            'parent_id': folder.parent.id if folder.parent else None
        }, status=201)


class PersonalStorageView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        folder_id = request.GET.get('folder')

        if folder_id:
            try:
                parent_folder = models.PersonalFolder.objects.get(id=folder_id, owner=user)
            except models.PersonalFolder.DoesNotExist:
                return Response({'error': 'Папка не найдена'}, status=404)
        else:
            parent_folder = None

        folders = models.PersonalFolder.objects.filter(owner=user, parent=parent_folder)
        documents = models.PersonalDocument.objects.filter(owner=user, folder=parent_folder)

        folder_list = [{
            'id': folder.id,
            'name': folder.name,
            'parent_id': folder.parent.id if folder.parent else None,
            'created_at': folder.created_at
        } for folder in folders]

        document_list = [{
            'id': doc.id,
            'name': doc.name,
            'description': doc.description,
            'folder_id': doc.folder.id if doc.folder else None,
            'created_at': doc.created_at,
            's3_key': doc.s3_key
        } for doc in documents]

        return Response({
            'current_folder': folder_id,
            'folders': folder_list,
            'documents': document_list
        }, status=200)

class PersonalFolderDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, folder_id):
        folder = get_object_or_404(models.PersonalFolder, id=folder_id, owner=request.user)
        serializer = models.PersonalFolderSerializer(folder)
        return Response(serializer.data)

class FamilyFolderDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        folder_id = request.GET.get('id')
        family_id = request.GET.get('family')
        family = models.Family.objects.get(id = family_id)
        folder = get_object_or_404(models.FamilyFolder, id=folder_id, family = family)
        serializer = models.FamilyFolderSerializer(folder)
        return Response(serializer.data)

class FamilyStorageView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        folder_id = request.GET.get('folder')
        family_id = request.GET.get('family_id')


        if not family_id:
            return Response({'error': 'Не передан family_id'}, status=400)

        try:
            membership = models.FamilyMembership.objects.get(user=user, family_id=family_id)
        except models.FamilyMembership.DoesNotExist:
            return Response({'error': 'Вы не состоите в этой семье'}, status=403)

        try:
            family = models.Family.objects.get(id=family_id)
        except models.Family.DoesNotExist:
            return Response({'error': 'Семья не найдена'}, status=404)

        if folder_id:
            try:
                parent = models.FamilyFolder.objects.get(id=folder_id, family=family)
            except models.FamilyFolder.DoesNotExist:
                return Response({'error': 'Папка не найдена'}, status=404)
        else:
            parent = None

        folders = models.FamilyFolder.objects.filter(family=family, parent=parent)
        documents = models.FamilyDocument.objects.filter(family=family, folder=parent)

        folder_list = [{
            'id': folder.id,
            'name': folder.name,
            'parent_id': folder.parent.id if folder.parent else None,
            'created_at': folder.created_at
        } for folder in folders]

        document_list = [{
            'id': doc.id,
            'name': doc.name,
            'description': doc.description,
            'folder_id': doc.folder.id if doc.folder else None,
            'created_at': doc.created_at,
            's3_key': doc.s3_key
        } for doc in documents]

        return Response({
            'current_folder': folder_id,
            'folders': folder_list,
            'documents': document_list
        }, status=200)

class SharePersonalDocumentWithFamilyView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        doc_id = request.data.get('document_id')
        family_id = request.data.get('family_id')
        folder_id = request.data.get('folder_id')  # опционально

        if not doc_id or not family_id:
            return Response({'error': 'document_id и family_id обязательны'}, status=400)

        try:
            personal_doc = models.PersonalDocument.objects.get(id=doc_id, owner=user)
        except models.PersonalDocument.DoesNotExist:
            return Response({'error': 'Документ не найден'}, status=404)

        try:
            family = models.Family.objects.get(id=family_id)
        except models.Family.DoesNotExist:
            return Response({'error': 'Семья не найдена'}, status=404)

        # Проверка, что пользователь состоит в семье
        if not models.FamilyMembership.objects.filter(user=user, family=family).exists():
            return Response({'error': 'Вы не состоите в этой семье'}, status=403)

        # Проверка папки
        folder = None
        if folder_id:
            try:
                folder = models.FamilyFolder.objects.get(id=folder_id, family=family)
            except models.FamilyFolder.DoesNotExist:
                return Response({'error': 'Папка не найдена'}, status=404)

        # Копирование файла в новое место в S3
        s3 = boto3.client('s3',
                          aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                          aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
                          region_name=settings.AWS_S3_REGION_NAME)

        source_key = personal_doc.s3_key
        file_extension = source_key.split('.')[-1]
        dest_key = f'family_docs/{family.id}/{uuid.uuid4()}.{file_extension}'

        try:
            copy_source = {
                'Bucket': settings.AWS_STORAGE_BUCKET_NAME,
                'Key': source_key
            }
            s3.copy_object(
                Bucket=settings.AWS_STORAGE_BUCKET_NAME,
                CopySource=copy_source,
                Key=dest_key
            )
        except Exception as e:
            return Response({'error': f'Ошибка копирования файла: {str(e)}'}, status=500)

        # Создание семейного документа
        family_doc = models.FamilyDocument.objects.create(
            family=family,
            name=personal_doc.name,
            description=personal_doc.description,
            s3_key=dest_key,
            folder=folder
        )

        serializer = models.FamilyDocumentSerializer(family_doc)
        return Response(serializer.data, status=201)

def share_personal_folder_with_family(personal_folder, family, user, target_family_folder=None):
    # Создаём новую семейную папку
    new_family_folder = models.FamilyFolder.objects.create(
        family=family,
        name=personal_folder.name,
        parent=target_family_folder
    )
    # Копируем документы в папке
    for personal_doc in personal_folder.personaldocument_set.all():
        # Новый ключ в S3
        file_extension = personal_doc.s3_key.split('.')[-1]
        new_key = f'family_docs/{family.id}/{uuid.uuid4()}.{file_extension}'
        # Копирование в S3
        s3 = boto3.client('s3',
                          aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                          aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
                          region_name=settings.AWS_S3_REGION_NAME)
        s3.copy_object(
            Bucket=settings.AWS_STORAGE_BUCKET_NAME,
            CopySource={
                'Bucket': settings.AWS_STORAGE_BUCKET_NAME,
                'Key': personal_doc.s3_key
            },
            Key=new_key
        )
        # Создание нового семейного документа
        models.FamilyDocument.objects.create(
            family=family,
            name=personal_doc.name,
            description=personal_doc.description,
            s3_key=new_key,
            folder=new_family_folder
        )
    # Рекурсивно копируем вложенные папки
    for child_folder in personal_folder.children.all():
        share_personal_folder_with_family(child_folder, family, user, target_family_folder=new_family_folder)

class SharePersonalFolderWithFamilyView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        folder_id = request.data.get('folder_id')
        family_id = request.data.get('family_id')
        target_folder_id = request.data.get('target_folder_id')  # куда вставить (опционально)

        if not folder_id or not family_id:
            return Response({'error': 'folder_id и family_id обязательны'}, status=400)

        try:
            personal_folder = models.PersonalFolder.objects.get(id=folder_id, owner=user)
        except models.PersonalFolder.DoesNotExist:
            return Response({'error': 'Папка не найдена'}, status=404)

        try:
            family = models.Family.objects.get(id=family_id)
        except models.Family.DoesNotExist:
            return Response({'error': 'Семья не найдена'}, status=404)

        # Проверка членства
        if not models.FamilyMembership.objects.filter(user=user, family=family).exists():
            return Response({'error': 'Вы не состоите в семье'}, status=403)

        # Целевая папка (опционально)
        target_folder = None
        if target_folder_id:
            try:
                target_folder = models.FamilyFolder.objects.get(id=target_folder_id, family=family)
            except models.FamilyFolder.DoesNotExist:
                return Response({'error': 'Целевая папка не найдена'}, status=404)

        # Копируем всё
        share_personal_folder_with_family(personal_folder, family, user, target_folder)
        return Response({'status': 'Папка успешно расшарена'}, status=201)

class UploadFamilyDocumentView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        file_obj = request.FILES.get('file')
        name = request.data.get('name', '').strip()
        description = request.data.get('description', '').strip()
        family_id = request.data.get('family_id')
        folder_id = request.data.get('folder_id')
        if not file_obj:
            return Response({'error': 'Файл не предоставлен'}, status=400)
        if not family_id:
            return Response({'error': 'Не передан family_id'}, status=400)
        # Проверка членства
        try:
            family = models.Family.objects.get(id=family_id)
            membership = models.FamilyMembership.objects.get(user=user, family=family)
        except models.Family.DoesNotExist:
            return Response({'error': 'Семья не найдена'}, status=404)
        except models.FamilyMembership.DoesNotExist:
            return Response({'error': 'Вы не состоите в этой семье'}, status=403)

        folder = None
        if folder_id:
            try:
                folder = models.FamilyFolder.objects.get(id=folder_id, family=family)
            except models.FamilyFolder.DoesNotExist:
                return Response({'error': 'Папка не найдена'}, status=404)

        if not name:
            base_name = "Новый документ"
            existing_docs = models.FamilyDocument.objects.filter(family=family, name__startswith=base_name).count()
            name = f"{base_name} ({existing_docs + 1})" if existing_docs else base_name
        elif len(name) > 255:
            return Response({'error': 'Имя документа слишком длинное (максимум 255 символов)'}, status=400)

        if description and len(description) > 500:
            return Response({'error': 'Описание слишком длинное (максимум 500 символов)'}, status=400)

        # Загружаем файл в S3
        file_extension = file_obj.name.split('.')[-1]
        s3_key = f'family_docs/{family.id}/{uuid.uuid4()}.{file_extension}'

        s3 = boto3.client('s3',
                          aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                          aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
                          region_name=settings.AWS_S3_REGION_NAME)

        try:
            s3.upload_fileobj(file_obj, settings.AWS_STORAGE_BUCKET_NAME, s3_key)
        except Exception as e:
            return Response({'error': f'Ошибка загрузки файла: {str(e)}'}, status=500)

        doc = models.FamilyDocument.objects.create(
            family=family,
            name=name,
            description=description,
            s3_key=s3_key,
            folder=folder
        )
        serializer = models.FamilyDocumentSerializer(doc)
        return Response(serializer.data, status=status.HTTP_201_CREATED)

class CreateFamilyFolderView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        name = request.data.get('name')
        family_id = request.data.get('family_id')
        parent_id = request.data.get('parent_id')

        if not name:
            return Response({'error': 'Название папки обязательно'}, status=400)

        if not family_id:
            return Response({'error': 'Не передан family_id'}, status=400)

        try:
            family = models.Family.objects.get(id=family_id)
            membership = models.FamilyMembership.objects.get(user=user, family=family)
        except models.Family.DoesNotExist:
            return Response({'error': 'Семья не найдена'}, status=404)
        except models.FamilyMembership.DoesNotExist:
            return Response({'error': 'Вы не состоите в этой семье'}, status=403)

        parent_folder = None
        if parent_id:
            try:
                parent_folder = models.FamilyFolder.objects.get(id=parent_id, family=family)
            except models.FamilyFolder.DoesNotExist:
                return Response({'error': 'Родительская папка не найдена'}, status=404)

        folder = models.FamilyFolder.objects.create(
            family=family,
            name=name,
            parent=parent_folder
        )
        return Response({
            'id': folder.id,
            'name': folder.name,
            'created_at': folder.created_at,
            'parent_id': folder.parent.id if folder.parent else None
        }, status=201)

class UpdatePersonalDocumentView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk):
        try:
            document = models.PersonalDocument.objects.get(id=pk, user=request.user)
        except models.PersonalDocument.DoesNotExist:
            return Response({'error': 'Документ не найден'}, status=404)

        name = request.data.get('name')
        description = request.data.get('description')
        folder_id = request.data.get('folder_id')

        if name:
            if len(name) > 255:
                return Response({'error': 'Имя слишком длинное'}, status=400)
            document.name = name

        if description:
            if len(description) > 500:
                return Response({'error': 'Описание слишком длинное'}, status=400)
            document.description = description

        if folder_id is not None:
            if folder_id == "":
                document.folder = None  # переместить в корень
            else:
                try:
                    folder = models.PersonalFolder.objects.get(id=folder_id, user=request.user)
                    document.folder = folder
                except models.PersonalFolder.DoesNotExist:
                    return Response({'error': 'Папка не найдена'}, status=404)

        document.save()
        return Response(models.PersonalDocumentSerializer(document).data)

class DeletePersonalDocumentView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def delete(self, request, pk):
        try:
            document = models.PersonalDocument.objects.get(id=pk, user=request.user)
        except models.PersonalDocument.DoesNotExist:
            return Response({'error': 'Документ не найден'}, status=404)

        s3 = boto3.client('s3',
                          aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                          aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
                          region_name=settings.AWS_S3_REGION_NAME)
        try:
            s3.delete_object(Bucket=settings.AWS_STORAGE_BUCKET_NAME, Key=document.s3_key)
        except Exception as e:
            return Response({'error': 'Не удалось удалить файл с S3'}, status=500)

        document.delete()
        return Response({'success': True}, status=204)


class OCRTextExtractionView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        try:
            doc = models.PersonalDocument.objects.get(id=pk, owner=request.user)
        except models.PersonalDocument.DoesNotExist:
            return Response({'error': 'Документ не найден'}, status=404)

        s3 = boto3.client('s3',
                          aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                          aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
                          region_name=settings.AWS_S3_REGION_NAME)
        obj = s3.get_object(Bucket=settings.AWS_STORAGE_BUCKET_NAME, Key=doc.s3_key)
        pdf_bytes = obj['Body'].read()

        images = convert_from_bytes(pdf_bytes)
        text = ""
        for page_image in images:
            text += pytesseract.image_to_string(page_image) + "\n"
        # Обрезаем до 100000 символов
        text = text[:100000]
        # Сохраняем в базе
        doc.recognized_text = text
        doc.save(update_fields=['recognized_text'])
        return Response({'text': text})



def download_pdf_from_s3(key):
    s3 = boto3.client('s3')
    pdf_file = BytesIO()
    s3.download_fileobj(settings.AWS_STORAGE_BUCKET_NAME, key, pdf_file)
    pdf_file.seek(0)
    return pdf_file

def extract_text_from_pdf(pdf_file):
    doc = fitz.open(stream=pdf_file.read(), filetype="pdf")
    full_text = ""
    for page in doc:
        full_text += page.get_text()
    return full_text

def translate_text(text, src='en', dest='ru'):
    translator = Translator()
    translated = translator.translate(text, src=src, dest=dest)
    return translated.text

class TranslateDocumentView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request, *args, **kwargs):
        document_id = request.data.get('document_id')
        is_family = request.data.get('is_family', False)
        src_lang = request.data.get('src_lang', 'auto')
        dest_lang = request.data.get('dest_lang', 'ru')

        if not document_id:
            return Response({'error': 'document_id is required'}, status=400)

        # Получаем документ
        model = models.FamilyDocument if is_family else models.PersonalDocument
        document = get_object_or_404(model, id=document_id)

        s3_key = document.s3_key  # Или как называется поле, хранящее путь к файлу

        try:
            pdf_file = download_pdf_from_s3(s3_key)
            text = extract_text_from_pdf(pdf_file)  # Или extract_text_with_ocr
            translated = translate_text(text, src=src_lang, dest=dest_lang)

            return Response({'translated_text': translated})

        except Exception as e:
            return Response({'error': str(e)}, status=500)


class GetPresignedDownloadLinkView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request):
        pk = request.query_params.get('pk')
        if pk is None:
            return Response({'error': 'Не указан pk'}, status=400)
        try:
            doc = models.PersonalDocument.objects.get(id=pk, owner=request.user)
        except models.PersonalDocument.DoesNotExist:
            return Response({'error': 'Документ не найден'}, status=404)

        s3 = boto3.client('s3',
                          aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                          aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
                          region_name=settings.AWS_S3_REGION_NAME)
        url = s3.generate_presigned_url(
            'get_object',
            Params={'Bucket': settings.AWS_STORAGE_BUCKET_NAME, 'Key': doc.s3_key},
            ExpiresIn=600  # 10 минут
        )
        print(url)
        return Response({'url': url})

class FamilyDocumentPresignedUrlView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request):
        pk = request.query_params.get('pk')
        if pk is None:
            return Response({'error': 'Не указан pk'}, status=400)
        # Получаем документ из семейных документов, проверяем, что пользователь в семье
        doc = get_object_or_404(models.FamilyDocument, id=pk)

        # Проверяем, что пользователь — член семьи этого документа
        if not models.FamilyMembership.objects.filter(family=doc.family, user=request.user).exists():
            return Response({"error": "Нет доступа к документу"}, status=403)

        s3 = boto3.client(
            's3',
            aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
            region_name=settings.AWS_S3_REGION_NAME
        )

        url = s3.generate_presigned_url(
            'get_object',
            Params={'Bucket': settings.AWS_STORAGE_BUCKET_NAME, 'Key': doc.s3_key},
            ExpiresIn=600  # Ссылка действует 10 мин
        )
        print(url)

        return Response({"url": url})

class DuplicateDocumentView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        try:
            doc = models.PersonalDocument.objects.get(id=pk, user=request.user)
        except models.PersonalDocument.DoesNotExist:
            return Response({'error': 'Документ не найден'}, status=404)

        s3 = boto3.client('s3',
                          aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                          aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
                          region_name=settings.AWS_S3_REGION_NAME)
        obj = s3.get_object(Bucket=settings.AWS_STORAGE_BUCKET_NAME, Key=doc.s3_key)
        original_file = s3.get_object(Bucket=settings.AWS_STORAGE_BUCKET_NAME, Key=doc.s3_key)['Body'].read()

        new_key = f'personal_docs/{request.user.id}/{uuid.uuid4()}.pdf'
        s3.upload_fileobj(BytesIO(original_file), settings.AWS_STORAGE_BUCKET_NAME, new_key)

        new_doc = models.PersonalDocument.objects.create(
            user=request.user,
            name=doc.name + " (копия)",
            description=doc.description,
            s3_key=new_key,
            folder=doc.folder
        )

        return Response(models.PersonalDocumentSerializer(new_doc).data, status=201)

class DuplicateFamilyDocumentView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        try:
            doc = models.FamilyDocument.objects.get(id=pk)
        except models.FamilyDocument.DoesNotExist:
            return Response({'error': 'Документ не найден'}, status=404)
        if not models.FamilyMembership.objects.filter(user=request.user, family=doc.family).exists():
            return Response({'error': 'Нет доступа к документу'}, status=403)
        s3 = boto3.client('s3',
                          aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                          aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
                          region_name=settings.AWS_S3_REGION_NAME)

        original_file = s3.get_object(Bucket=settings.AWS_STORAGE_BUCKET_NAME, Key=doc.s3_key)['Body'].read()
        new_key = f'family_docs/{doc.family.id}/{uuid.uuid4()}.pdf'
        s3.upload_fileobj(BytesIO(original_file), settings.AWS_STORAGE_BUCKET_NAME, new_key)
        new_doc = models.FamilyDocument.objects.create(
            family=doc.family,
            name=doc.name + " (копия)",
            description=doc.description,
            s3_key=new_key,
            folder=doc.folder,
            recognized_text=doc.recognized_text
        )

        return Response(models.FamilyDocumentSerializer(new_doc).data, status=201)

class FamilyOCRTextExtractionView(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        user = request.user
        try:
            doc = models.FamilyDocument.objects.get(id=pk)
        except models.FamilyDocument.DoesNotExist:
            return Response({'error': 'Документ не найден'}, status=404)

        # Проверяем членство пользователя в семье
        if not models.FamilyMembership.objects.filter(user=user, family=doc.family).exists():
            return Response({'error': 'Доступ запрещён'}, status=403)

        s3 = boto3.client('s3',
                          aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                          aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
                          region_name=settings.AWS_S3_REGION_NAME)
        obj = s3.get_object(Bucket=settings.AWS_STORAGE_BUCKET_NAME, Key=doc.s3_key)
        pdf_bytes = obj['Body'].read()

        images = convert_from_bytes(pdf_bytes)
        text = ""
        for page_image in images:
            text += pytesseract.image_to_string(page_image) + "\n"

        text = text[:100000]  # Обрезаем

        doc.recognized_text = text
        doc.save(update_fields=['recognized_text'])

        return Response({'text': text})
