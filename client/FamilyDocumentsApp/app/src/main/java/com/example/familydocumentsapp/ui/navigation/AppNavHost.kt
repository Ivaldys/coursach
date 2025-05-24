package com.example.familydocumentsapp.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.familydocumentsapp.ui.screens.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onEmailNotConfirmed = { email ->
                    navController.navigate("confirm_code/$email") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                navController = navController,
                onBackToLogin = {
                    navController.popBackStack()
                },
                onRecoverySuccess = { email ->
                    navController.navigate("forgot_password_confirm/$email") {
                        popUpTo("forgot_password") { inclusive = true }
                    }
                }
            )
        }
        composable("forgot_password_confirm/{email}") {backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ForgotPasswordConfirmScreen(
                email = email,
                navController = navController,
                onConfirmSuccess = {
                    navController.navigate("login") {
                        popUpTo("forgot_password_confirm") { inclusive = true }
                    }
                },
                onBackToRecover = {
                    navController.navigate("forgot_password") {
                        popUpTo("forgot_password_confirm") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { email ->
                    // Переход на экран подтверждения кода (создадим потом)
                    navController.navigate("confirm_code/$email") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }
        composable("confirm_code/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ConfirmCodeScreen(
                email = email,
                navController = navController,
                onConfirmSuccess = {
                    navController.navigate("login") {
                        popUpTo("confirm_code") { inclusive = true }
                    }
                },
                onBackToRegister = {
                    navController.navigate("register") {
                        popUpTo("confirm_code") { inclusive = true }
                    }
                }

            )
        }
        composable("home") {
            HomeScreen(
                navController = navController,
                onProfileClicked = {
                    navController.navigate("profile") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onFamilySelected = { family ->
                    navController.navigate("family_home/$family") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onLogoutClicked = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
            )
        }
        composable("profile") {
            ProfileScreen(
                navController = navController,
                onFamilySelected = { family ->
                    navController.navigate("family_home/$family") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onLogoutClicked = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                navToHome = {
                    navController.navigate("home") {
                    popUpTo("profile") { inclusive = true }
                } },
                onEditEmailClicked = {
                    navController.navigate("edit_email") {
                        popUpTo("profile") { inclusive = true }
                    }
                },
                onEditPasswordClicked = {
                    navController.navigate("edit_password") {
                        popUpTo("profile") { inclusive = true }
                    }
                },
                onHomeClicked = {},

            )
        }
        composable("family_home/{family}") { backStackEntry ->
            val family = backStackEntry.arguments?.getString("family") ?: ""
            FamilyHomeScreen (
                family = family,
                navController = navController,
                onFamilySelected = { family ->
                    navController.navigate("family_home/$family") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onLogoutClicked = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onProfileClicked = {
                    navController.navigate("profile") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                navToHome = {
                    navController.navigate("home") {
                        popUpTo("profile") { inclusive = true }
                    } },
            )
        }
        composable("edit_email") {
            ChangeEmailScreen(
                navController = navController,
                navProfile = {
                    navController.navigate("profile") {
                        popUpTo("edit_email") { inclusive = true }
                    }},
                onChangeSuccess = {
                    navController.navigate("edit_email_confirm") {
                        popUpTo("edit_email") { inclusive = true }
                    }
                },
                )
        }
        composable("edit_email_confirm") {
            ChangeEmailConfirmScreen(
                navController = navController,
                onBackToChange = {
                    navController.navigate("edit_email") {
                        popUpTo("edit_email_confirm") { inclusive = true }
                    }},
                onConfirmSuccess = {
                    navController.navigate("profile") {
                        popUpTo("edit_email_confirm") { inclusive = true }
                    }
                },
            )
        }
        composable("edit_password") {
            ChangePasswordScreen(
                navController = navController,
                navProfile = {
                    navController.navigate("profile") {
                        popUpTo("edit_password") { inclusive = true }
                    }},
                onChangeSuccess = { new_password ->
                    navController.navigate("edit_password_confirm/$new_password") {
                        popUpTo("edit_password") { inclusive = true }
                    }
                },
            )
        }
        composable("edit_password_confirm/{new_password}") { backStackEntry ->
            val new_password = backStackEntry.arguments?.getString("new_password") ?: ""
            ChangePasswordConfirmScreen(
                new_password=new_password,
                navController = navController,
                onBackToChange = {
                    navController.navigate("edit_password") {
                        popUpTo("edit_password_confirm") { inclusive = true }
                    }},
                onConfirmSuccess = {
                    navController.navigate("profile") {
                        popUpTo("edit_password_confirm") { inclusive = true }
                    }
                },
            )
        }

    }
}