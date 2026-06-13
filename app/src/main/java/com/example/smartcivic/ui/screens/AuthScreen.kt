package com.example.smartcivic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartcivic.data.repository.CivicRepository
import com.example.smartcivic.theme.Teal400
import com.example.smartcivic.theme.Rose500
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: CivicRepository) : ViewModel() {
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var role by mutableStateOf("CITIZEN") // CITIZEN, WORKER, ADMIN
    var isRegisterMode by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun login(onSuccess: (String) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            val result = repository.signIn(email, password)
            isLoading = false
            result.onSuccess { user ->
                onSuccess(user.role)
            }.onFailure { err ->
                errorMessage = err.message ?: "Authentication failed"
            }
        }
    }

    fun register(onSuccess: (String) -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            val result = repository.signUp(name, email, password, role)
            isLoading = false
            result.onSuccess { user ->
                onSuccess(user.role)
            }.onFailure { err ->
                errorMessage = err.message ?: "Registration failed"
            }
        }
    }
}

@Composable
fun AuthScreen(
    repository: CivicRepository,
    onAuthSuccess: (String) -> Unit,
    preselectedRole: String = "CITIZEN",
    preselectedRegister: Boolean = false,
    modifier: Modifier = Modifier
) {
    val viewModel: AuthViewModel = viewModel { AuthViewModel(repository) }
    
    // Sync preselected params to view model on entry
    androidx.compose.runtime.LaunchedEffect(preselectedRole, preselectedRegister) {
        viewModel.role = preselectedRole.uppercase()
        viewModel.isRegisterMode = preselectedRegister
    }
    
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF072115), // Deep Forest Green (Chime dark)
            Color(0xFF0D3E27),
            Color(0xFF135B3B)  // Dark Minty Green
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo/Header
            Text(
                text = "smart civic",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Teal400, // Mint Green
                letterSpacing = (-1.5).sp
            )
            Text(
                text = "Smart Governance & Leaderboard",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.LightGray.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Glassmorphic Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x2A153A2A) // Translucent deep green card
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tab Headers: Login / Register
                    val tabIndex = if (viewModel.isRegisterMode) 1 else 0
                    TabRow(
                        selectedTabIndex = tabIndex,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                                color = Teal400
                            )
                        }
                    ) {
                        Tab(
                            selected = tabIndex == 0,
                            onClick = { 
                                viewModel.isRegisterMode = false
                                viewModel.errorMessage = null
                            },
                            text = { Text("Log In", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = tabIndex == 1,
                            onClick = { 
                                viewModel.isRegisterMode = true
                                viewModel.errorMessage = null
                            },
                            text = { Text("Register", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (viewModel.isRegisterMode) {
                        // Role Selector for Register
                        var roleIndex by remember(viewModel.role) { 
                            mutableIntStateOf(
                                when(viewModel.role.uppercase()) {
                                    "WORKER" -> 1
                                    "ADMIN" -> 2
                                    else -> 0
                                }
                            ) 
                        }
                        Text(
                            text = "Register As:",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { 
                                    roleIndex = 0
                                    viewModel.role = "CITIZEN"
                                },
                                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (roleIndex == 0) Teal400 else Color.Transparent,
                                    contentColor = if (roleIndex == 0) Color.Black else Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = roleIndex != 0),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Citizen", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { 
                                    roleIndex = 1
                                    viewModel.role = "WORKER"
                                },
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (roleIndex == 1) Teal400 else Color.Transparent,
                                    contentColor = if (roleIndex == 1) Color.Black else Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = roleIndex != 1),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Worker", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { 
                                    roleIndex = 2
                                    viewModel.role = "ADMIN"
                                },
                                shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (roleIndex == 2) Teal400 else Color.Transparent,
                                    contentColor = if (roleIndex == 2) Color.Black else Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = roleIndex != 2),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Admin", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        // Name Field
                        OutlinedTextField(
                            value = viewModel.name,
                            onValueChange = { viewModel.name = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Teal400,
                                unfocusedLabelColor = Color.Gray,
                                focusedBorderColor = Teal400,
                                unfocusedBorderColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Role indicator for Login
                        Text(
                            text = "Logging in as: ${viewModel.role.lowercase().replaceFirstChar { it.uppercase() }}",
                            color = Teal400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Email Field
                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = { viewModel.email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Teal400,
                            unfocusedLabelColor = Color.Gray,
                            focusedBorderColor = Teal400,
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Password Field
                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = { viewModel.password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Teal400,
                            unfocusedLabelColor = Color.Gray,
                            focusedBorderColor = Teal400,
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Message Display
                    AnimatedVisibility(
                        visible = viewModel.errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        viewModel.errorMessage?.let {
                            Text(
                                text = it,
                                color = Rose500,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }

                    // Loading indicator / Action Button
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(color = Teal400, modifier = Modifier.size(32.dp))
                    } else {
                        Button(
                            onClick = {
                                if (viewModel.isRegisterMode) {
                                    viewModel.register(onAuthSuccess)
                                } else {
                                    viewModel.login(onAuthSuccess)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Teal400,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (viewModel.isRegisterMode) "Create Account" else "Log In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick-switch backends/demo user hints (Handy for testing)
                    Text(
                        text = "Demo accounts:\ncitizen@gmail.com | worker@civic.gov | admin@civic.gov\nPassword: any",
                        color = Color.LightGray.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}
