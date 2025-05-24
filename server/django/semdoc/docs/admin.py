from django.contrib import admin
from .models import (
    MyUser,
    Family,
    FamilyMembership,
    FamilyJoinRequest,
    PersonalFolder,
    FamilyFolder,
    PersonalDocument,
    FamilyDocument
)


# Register your models here.
admin.site.register(MyUser)
admin.site.register(Family)
admin.site.register(FamilyMembership)
admin.site.register(FamilyJoinRequest)
admin.site.register(PersonalFolder)
admin.site.register(FamilyFolder)
admin.site.register(PersonalDocument)
admin.site.register(FamilyDocument)