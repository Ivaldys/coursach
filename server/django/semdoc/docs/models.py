from django.db import models
import random
from django.utils import timezone
from datetime import timedelta
from django.contrib.auth import get_user_model
from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
from django.contrib.auth.hashers import make_password
from rest_framework import serializers

class MyUserManager(BaseUserManager):
    def create_user(self, email, name, password=None):
        if not email:
            raise ValueError("Email обязателен")

        email = self.normalize_email(email)
        user = self.model(email=email, name=name)
        user.set_password(password)  # хеширует пароль
        user.save(using=self._db)
        return user

    def create_superuser(self, email, name, password):
        user = self.create_user(email, name, password)
        user.is_staff = True
        user.is_superuser = True
        user.save(using=self._db)
        return user


class MyUser(AbstractBaseUser, PermissionsMixin):
    name = models.CharField(max_length=100)
    email = models.EmailField(unique=True)
    password = models.CharField(max_length=128)
    email_confirmed = models.BooleanField(default=False)
    confirmation_code = models.CharField(max_length=4, blank=True, null=True)
    code_created_at = models.DateTimeField(blank=True, null=True)
    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)
    new_email = models.EmailField(blank=True, null=True)
    email_change_code = models.CharField(max_length=4, blank=True, null=True)
    email_change_code_created_at = models.DateTimeField(blank=True, null=True)

    password_change_code = models.CharField(max_length=4, blank=True, null=True)
    password_change_code_created_at = models.DateTimeField(blank=True, null=True)

    password_recovery_code = models.CharField(max_length=4, blank=True, null=True)
    password_recovery_code_created_at = models.DateTimeField(blank=True, null=True)


    objects = MyUserManager()

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['name']


    def __str__(self):
        return self.email

    def generate_confirmation_code(self):
        self.confirmation_code  = f"{random.randint(1000, 9999)}"
        self.code_created_at = timezone.now()
        self.save()

    def is_code_expired(self):
        if not self.code_created_at:
            return True
        return timezone.now() > self.code_created_at + timedelta(minutes=15)

User = get_user_model()

class Family(models.Model):
    name = models.CharField(max_length=100)
    join_password = models.CharField(max_length=128)  # хешированный
    created_at = models.DateTimeField(auto_now_add=True)

    def set_password(self, raw_password):
        self.join_password = make_password(raw_password)

    def __str__(self):
        return f"{self.name} (ID: {self.id})"

class FamilyMembership(models.Model):
    ROLE_CHOICES = (
        ('owner', 'Владелец'),
        ('member', 'Участник'),
    )
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='family_memberships')
    family = models.ForeignKey(Family, on_delete=models.CASCADE, related_name='memberships')
    role = models.CharField(max_length=10, choices=ROLE_CHOICES, default='member')
    joined_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('user', 'family')  # один пользователь не может дважды вступить в одну семью
    def __str__(self):
        return f"{self.user.email} в {self.family.name} как {self.role}"

class FamilyJoinRequest(models.Model):
    STATUS_CHOICES = (
        ('pending', 'В ожидании'),
        ('accepted', 'Принята'),
        ('rejected', 'Отклонена'),
    )

    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='join_requests')
    family = models.ForeignKey(Family, on_delete=models.CASCADE, related_name='join_requests')
    status = models.CharField(max_length=10, choices=STATUS_CHOICES, default='pending')
    created_at = models.DateTimeField(auto_now_add=True)
    resolved_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        unique_together = ('user', 'family')  # одна заявка на семью от пользователя

    def __str__(self):
        return f"{self.user.email} → {self.family.name} [{self.status}]"

class PersonalFolder(models.Model):
    owner = models.ForeignKey(MyUser, on_delete=models.CASCADE, related_name='personal_folders')
    name = models.CharField(max_length=255)
    parent = models.ForeignKey('self', null=True, blank=True, on_delete=models.CASCADE, related_name='subfolders')
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.name} (владелец: {self.owner.email})"

class FamilyFolder(models.Model):
    family = models.ForeignKey(Family, on_delete=models.CASCADE, related_name='folders')
    name = models.CharField(max_length=255)
    parent = models.ForeignKey('self', null=True, blank=True, on_delete=models.CASCADE, related_name='subfolders')
    created_at = models.DateTimeField(auto_now_add=True)
    shared_by = models.ForeignKey(MyUser, null=True, blank=True, on_delete=models.SET_NULL, related_name='shared_family_folders')

    def __str__(self):
        return f"{self.name} (семья: {self.family.name})"

class PersonalDocument(models.Model):
    owner = models.ForeignKey(MyUser, on_delete=models.CASCADE, related_name='personal_documents')
    name = models.CharField(max_length=255)
    description = models.TextField(max_length=500, blank=True)
    recognized_text = models.TextField(blank=True, null=True)
    s3_key = models.CharField(max_length=512)  # путь в S3
    created_at = models.DateTimeField(auto_now_add=True)
    folder = models.ForeignKey('PersonalFolder', null=True, blank=True, on_delete=models.SET_NULL, related_name='documents')

class FamilyDocument(models.Model):
    family = models.ForeignKey(Family, on_delete=models.CASCADE, related_name='documents')
    name = models.CharField(max_length=255)
    description = models.TextField(max_length=500, blank=True)
    recognized_text = models.TextField(blank=True, null=True)
    s3_key = models.CharField(max_length=512)
    created_at = models.DateTimeField(auto_now_add=True)
    folder = models.ForeignKey('FamilyFolder', null=True, blank=True, on_delete=models.SET_NULL, related_name='documents')
    shared_by = models.ForeignKey(MyUser, null=True, blank=True, on_delete=models.SET_NULL, related_name='shared_documents')  # кто расшарил

class PersonalDocumentSerializer(serializers.ModelSerializer):
    class Meta:
        model = PersonalDocument
        fields = ['id', 'name', 'description', 's3_key', 'created_at', 'folder']

class FamilyDocumentSerializer(serializers.ModelSerializer):
    class Meta:
        model = FamilyDocument
        fields = ['id', 'name', 'description', 's3_key', 'created_at', 'folder', 'family']

class PersonalFolderSerializer(serializers.ModelSerializer):
    serializers.PrimaryKeyRelatedField(read_only=True)

    class Meta:
        model = PersonalFolder
        fields = ['id', 'name', 'created_at', 'parent']

class FamilyFolderSerializer(serializers.ModelSerializer):
    serializers.PrimaryKeyRelatedField(read_only=True)

    class Meta:
        model = FamilyFolder
        fields = ['id', 'name', 'created_at', 'parent']