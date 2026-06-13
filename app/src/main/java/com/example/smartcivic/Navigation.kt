package com.example.smartcivic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.smartcivic.data.repository.CivicRepository
import com.example.smartcivic.ui.screens.AuthScreen
import com.example.smartcivic.ui.screens.CitizenDashboard
import com.example.smartcivic.ui.screens.WorkerDashboard
import com.example.smartcivic.ui.screens.AdminDashboard
import com.example.smartcivic.ui.screens.HomeScreen

@Composable
fun MainNavigation(repository: CivicRepository) {
  val backStack = rememberNavBackStack(Home)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Home> {
          HomeScreen(
            repository = repository,
            onNavigateToAuth = { role, isReg -> backStack.add(Auth(role, isReg)) },
            onNavigateToCitizen = { backStack.add(CitizenHome) },
            onNavigateToWorker = { backStack.add(WorkerHome) },
            onNavigateToAdmin = { backStack.add(AdminHome) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Auth> { authKey ->
          AuthScreen(
            repository = repository,
            preselectedRole = authKey.role,
            preselectedRegister = authKey.isRegister,
            onAuthSuccess = { role ->
              when (role.uppercase()) {
                "CITIZEN" -> backStack.add(CitizenHome)
                "WORKER" -> backStack.add(WorkerHome)
                "ADMIN" -> backStack.add(AdminHome)
              }
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<CitizenHome> {
          CitizenDashboard(
            repository = repository,
            onLogout = { backStack.add(Home) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<WorkerHome> {
          WorkerDashboard(
            repository = repository,
            onLogout = { backStack.add(Home) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<AdminHome> {
          AdminDashboard(
            repository = repository,
            onLogout = { backStack.add(Home) },
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
