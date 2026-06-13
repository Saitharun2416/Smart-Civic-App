package com.example.smartcivic

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data class Auth(val role: String = "CITIZEN", val isRegister: Boolean = false) : NavKey
@Serializable data object CitizenHome : NavKey
@Serializable data object WorkerHome : NavKey
@Serializable data object AdminHome : NavKey
