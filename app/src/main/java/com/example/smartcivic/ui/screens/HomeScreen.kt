package com.example.smartcivic.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import com.example.smartcivic.data.models.Complaint
import com.example.smartcivic.data.models.UserData
import com.example.smartcivic.data.repository.CivicRepository
import com.example.smartcivic.theme.AppSettings
import com.example.smartcivic.theme.ChimeDarkGreen
import com.example.smartcivic.theme.ChimeForestGreen
import com.example.smartcivic.theme.ChimeGreen
import com.example.smartcivic.theme.ChimeLightGreenBg
import com.example.smartcivic.theme.Rose500
import com.example.smartcivic.theme.Slate400
import com.example.smartcivic.theme.Slate600
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: CivicRepository,
    onNavigateToAuth: (String, Boolean) -> Unit,
    onNavigateToCitizen: () -> Unit,
    onNavigateToWorker: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSplashShowing by remember { mutableStateOf(true) }
    var currentTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Explore, 2: Report, 3: Leaders, 4: Settings
    var isLoginDropdownExpanded by remember { mutableStateOf(false) }
    
    val currentUser by repository.observeCurrentUser().collectAsState(initial = repository.getCurrentUser())
    val complaints by repository.observeComplaints().collectAsState(initial = emptyList())
    val leaderboard by repository.observeLeaderboard().collectAsState(initial = emptyList())
    
    val coroutineScope = rememberCoroutineScope()

    // Trigger splash timeout
    LaunchedEffect(Unit) {
        delay(2000)
        isSplashShowing = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main Application Scaffold
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(ChimeGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ChimeDarkGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "smart civic",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                color = if (AppSettings.isDarkMode.value) Color.White else ChimeForestGreen
                            )
                        }
                    },
                    actions = {
                        // Profile Quicklink
                        IconButton(onClick = { currentTab = 4 }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = if (AppSettings.isDarkMode.value) Color.White else Slate600
                            )
                        }
                        // Settings Quicklink
                        IconButton(onClick = { currentTab = 4 }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (AppSettings.isDarkMode.value) Color.White else Slate600
                            )
                        }
                        
                        // Login Dropdown Trigger
                        Box {
                            val user = currentUser
                            if (user != null) {
                                Button(
                                    onClick = { isLoginDropdownExpanded = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ChimeGreen,
                                        contentColor = ChimeDarkGreen
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = user.name.split(" ").firstOrNull() ?: "Dashboard",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isLoginDropdownExpanded,
                                    onDismissRequest = { isLoginDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Open Dashboard", fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                        onClick = {
                                            isLoginDropdownExpanded = false
                                            when (user.role.uppercase()) {
                                                "CITIZEN" -> onNavigateToCitizen()
                                                "WORKER" -> onNavigateToWorker()
                                                "ADMIN" -> onNavigateToAdmin()
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sign Out", color = Rose500) },
                                        leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Rose500) },
                                        onClick = {
                                            isLoginDropdownExpanded = false
                                            coroutineScope.launch {
                                                repository.signOut()
                                            }
                                        }
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { isLoginDropdownExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ChimeGreen),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (AppSettings.isDarkMode.value) ChimeGreen else ChimeForestGreen
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("Log In", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isLoginDropdownExpanded,
                                    onDismissRequest = { isLoginDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Login as Citizen") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        onClick = {
                                            isLoginDropdownExpanded = false
                                            onNavigateToAuth("CITIZEN", false)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Login as Worker") },
                                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                                        onClick = {
                                            isLoginDropdownExpanded = false
                                            onNavigateToAuth("WORKER", false)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Login as Administrator") },
                                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                        onClick = {
                                            isLoginDropdownExpanded = false
                                            onNavigateToAuth("ADMIN", false)
                                        }
                                    )
                                    Divider()
                                    DropdownMenuItem(
                                        text = { Text("Register Account", fontWeight = FontWeight.Bold, color = ChimeForestGreen) },
                                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = ChimeForestGreen) },
                                        onClick = {
                                            isLoginDropdownExpanded = false
                                            onNavigateToAuth("CITIZEN", true)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChimeDarkGreen,
                            selectedTextColor = ChimeForestGreen,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400,
                            indicatorColor = ChimeGreen
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Explore") },
                        label = { Text("Explore", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChimeDarkGreen,
                            selectedTextColor = ChimeForestGreen,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400,
                            indicatorColor = ChimeGreen
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Report") },
                        label = { Text("Report", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChimeDarkGreen,
                            selectedTextColor = ChimeForestGreen,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400,
                            indicatorColor = ChimeGreen
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Leaderboard") },
                        label = { Text("Leaderboard", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChimeDarkGreen,
                            selectedTextColor = ChimeForestGreen,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400,
                            indicatorColor = ChimeGreen
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChimeDarkGreen,
                            selectedTextColor = ChimeForestGreen,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400,
                            indicatorColor = ChimeGreen
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentTab) {
                    0 -> HomeTabContent(
                        currentUser = currentUser,
                        complaints = complaints,
                        onNavigateToReport = { currentTab = 2 },
                        onNavigateToAuth = { onNavigateToAuth("CITIZEN", true) }
                    )
                    1 -> ExploreTabContent(complaints = complaints)
                    2 -> ReportTabContent(
                        currentUser = currentUser,
                        repository = repository,
                        onNavigateToAuth = { onNavigateToAuth("CITIZEN", false) }
                    )
                    3 -> LeaderboardTabContent(leaderboard = leaderboard)
                    4 -> SettingsTabContent(
                        currentUser = currentUser,
                        onSignOut = {
                            coroutineScope.launch {
                                repository.signOut()
                            }
                        },
                        onNavigateToAuth = { onNavigateToAuth("CITIZEN", false) }
                    )
                }
            }
        }

        // Animated Splash Screen overlay
        AnimatedVisibility(
            visible = isSplashShowing,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            SplashLayout()
        }
    }
}

// ---------------- SPLASH LAYOUT ----------------
@Composable
fun SplashLayout() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChimeDarkGreen),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ChimeGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ChimeDarkGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "smart civic",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = ChimeGreen,
                letterSpacing = (-2).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "your city. your response.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ChimeLightGreenBg.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }
    }
}

// ---------------- TAB 0: HOME CONTENT ----------------
@Composable
fun HomeTabContent(
    currentUser: UserData?,
    complaints: List<Complaint>,
    onNavigateToReport: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Welcome Header
        Text(
            text = if (currentUser != null) "Hi, ${currentUser.name.split(" ").first()}!" else "Welcome to smart civic",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Together, let's keep our neighborhoods pristine.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // SpotCivic Card (Inspired by Chime's card screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(ChimeGreen, ChimeForestGreen)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "smart civic ID",
                        color = ChimeDarkGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (currentUser != null) currentUser.role.uppercase() else "GUEST",
                            color = ChimeDarkGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column {
                    Text(
                        text = if (currentUser != null) "${currentUser.points} PTS" else "0 PTS",
                        color = ChimeDarkGreen,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = if (currentUser != null) "Level ${currentUser.rank} Citizen" else "Sign in to earn rewards",
                        color = ChimeDarkGreen.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (currentUser != null) currentUser.name.uppercase() else "GUEST CITIZEN",
                        color = ChimeDarkGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "VIZ",
                        color = ChimeDarkGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Interactive Horizontal Carousel (Like Chime's card color picker UI)
        Text(
            text = "Quick Opportunities",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                CarouselBannerCard(
                    title = "Spot & Earn",
                    subtitle = "Get +100 points for reporting potholes",
                    icon = Icons.Default.AddCircle,
                    color = ChimeLightGreenBg,
                    textColor = ChimeForestGreen,
                    onClick = onNavigateToReport
                )
            }
            item {
                CarouselBannerCard(
                    title = "Join SpotCivic",
                    subtitle = "Register an account to log issues",
                    icon = Icons.Default.Person,
                    color = Color(0xFFFFF3CD),
                    textColor = Color(0xFF856404),
                    onClick = onNavigateToAuth
                )
            }
            item {
                CarouselBannerCard(
                    title = "Civic Leaderboards",
                    subtitle = "Compete with neighbors & workers",
                    icon = Icons.Default.Star,
                    color = Color(0xFFCCE5FF),
                    textColor = Color(0xFF004085),
                    onClick = {}
                )
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // City Analytics stats grid
        Text(
            text = "City Analytics",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                value = "1,850+",
                label = "Issues Solved",
                icon = Icons.Default.CheckCircle,
                color = ChimeGreen,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = "98.4%",
                label = "Satisfaction Rate",
                icon = Icons.Default.Star,
                color = Color(0xFFFBBF24),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                value = "< 24h",
                label = "Avg Response",
                icon = Icons.Default.Info,
                color = ChimeForestGreen,
                modifier = Modifier.weight(1f)
            )
            val pendingCount = complaints.count { it.status == "Pending" }
            MetricCard(
                value = pendingCount.toString(),
                label = "Active Complaints",
                icon = Icons.Default.Warning,
                color = Rose500,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Dynamic Civic bulletins
        Text(
            text = "Recent Updates",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sector 4 Road Repair Complete",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Workers solved the heavy pothole cluster reported by citizens yesterday. Thanks to municipal worker Arjun!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Garbage Free Drive in Sector 8",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "A major garbage overflow has been resolved. Let's keep our environment clean. Citizen reviews rated this 5 stars.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun CarouselBannerCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.8f),
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MetricCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---------------- TAB 1: EXPLORE CONTENT ----------------
@Composable
fun ExploreTabContent(complaints: List<Complaint>) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    val categories = listOf("All", "Pothole", "Garbage", "Water", "Drainage", "Streetlight", "Traffic")
    
    val filteredComplaints = complaints.filter { complaint ->
        val matchesSearch = complaint.title.contains(searchQuery, ignoreCase = true) || 
                            complaint.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || 
                              complaint.category.contains(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Explore Issues",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Track active municipal problem resolutions in your town.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search issues...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Category selection row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) ChimeGreen else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isSelected) ChimeGreen else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) ChimeDarkGreen else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Feed Items
        if (filteredComplaints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No issues match your criteria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredComplaints) { complaint ->
                    ComplaintFeedItem(complaint = complaint)
                }
            }
        }
    }
}

@Composable
fun ComplaintFeedItem(complaint: Complaint) {
    val statusColor = when (complaint.status) {
        "Pending" -> Color(0xFFFFB300)
        "In Progress" -> Color(0xFF1E88E5)
        "Resolved" -> Color(0xFF43A047)
        else -> Color.Gray
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = complaint.category.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = ChimeForestGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = complaint.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = complaint.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = complaint.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (complaint.imageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = complaint.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = complaint.address,
                    fontSize = 11.sp,
                    color = Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------- TAB 2: REPORT COMPLAINT ----------------
@Composable
fun ReportTabContent(
    currentUser: UserData?,
    repository: CivicRepository,
    onNavigateToAuth: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    if (currentUser == null || currentUser.role.uppercase() != "CITIZEN") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ChimeForestGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Citizen Report Access Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "You must be logged in as a Citizen User to file reports and earn civic points.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToAuth,
                        colors = ButtonDefaults.buttonColors(containerColor = ChimeGreen, contentColor = ChimeDarkGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Log In as Citizen", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // File report form
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Pothole") }
        var imageUrl by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var success by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        
        val categories = listOf("Pothole", "Streetlight", "Garbage", "Water Leak", "Drainage", "Traffic")

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                if (uri != null) {
                    imageUrl = uri.toString()
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Report Local Issue",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Describe the problem and earn points when it resolves.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (success) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ChimeLightGreenBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ChimeForestGreen, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Submission Successful!", fontWeight = FontWeight.Bold, color = ChimeDarkGreen)
                            Text("Your report has been dispatched to local workers.", fontSize = 11.sp, color = ChimeDarkGreen, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    success = false
                                    title = ""
                                    description = ""
                                    imageUrl = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ChimeForestGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Report Another")
                            }
                        }
                    }
                }
            } else {
                item {
                    Text("Select Category", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(categories) { cat ->
                            val isSelected = category == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ChimeGreen else MaterialTheme.colorScheme.surface)
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ChimeDarkGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Short Summary / Title") },
                        placeholder = { Text("e.g. Broken streetlight on 4th Ave") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe Details") },
                        placeholder = { Text("Provide any details that will help workers locate/fix...") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 3
                    )
                }
                
                item {
                    Text("Attach Photo of the Issue", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (imageUrl.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Select Photo",
                                    tint = ChimeForestGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Select Photo from Gallery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "PNG or JPG, up to 10MB",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Selected Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.5f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.3f)
                                            )
                                        )
                                    )
                            )
                            IconButton(
                                onClick = { imageUrl = "" },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Photo",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Photo Selected",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            )
                        }
                    }
                }
                
                item {
                    errorMsg?.let {
                        Text(it, color = Color.Red, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isSubmitting) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ChimeGreen)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (title.isBlank() || description.isBlank()) {
                                    errorMsg = "Please fill in title and description"
                                    return@Button
                                }
                                isSubmitting = true
                                errorMsg = null
                                coroutineScope.launch {
                                    val result = repository.reportComplaint(
                                        title = title,
                                        description = description,
                                        category = category,
                                        imageUrl = imageUrl,
                                        latitude = 12.9715,
                                        longitude = 77.5945,
                                        address = "Richmond Circle, Bengaluru"
                                    )
                                    isSubmitting = false
                                    result.onSuccess {
                                        success = true
                                    }.onFailure {
                                        errorMsg = it.message ?: "Failed to report issue"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChimeGreen, contentColor = ChimeDarkGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Complaint", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 3: LEADERBOARD CONTENT ----------------
@Composable
fun LeaderboardTabContent(leaderboard: List<UserData>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Top Solvers",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Active municipal workers ranked by solved complaints.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (leaderboard.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No solvers ranked yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(leaderboard) { index, worker ->
                    val rank = index + 1
                    val rankColor = when (rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> Color.Transparent
                    }
                    
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank number
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (rank <= 3) rankColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rank.toString(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = if (rank <= 3) ChimeDarkGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = worker.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${worker.issuesSolved} resolved",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (worker.ratingCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                                        Text(
                                            text = " %.1f".format(worker.averageRating),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                text = "${worker.points} pts",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = ChimeForestGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 4: SETTINGS & PROFILE CONTENT ----------------
@Composable
fun SettingsTabContent(
    currentUser: UserData?,
    onSignOut: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Profile & Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Profile Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (currentUser != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(ChimeLightGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.name.take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChimeForestGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = currentUser.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = currentUser.email,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Account Role", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentUser.role.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Civic Points", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${currentUser.points} pts", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ChimeForestGreen)
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text("You are browsing as a Guest", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "Login to report local issues, track submissions, and build points to level up.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Button(
                            onClick = onNavigateToAuth,
                            colors = ButtonDefaults.buttonColors(containerColor = ChimeGreen, contentColor = ChimeDarkGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sign In Now")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Settings Toggles
        Text(
            text = "App Settings",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Dark Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Toggle custom forest-dark mode", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = AppSettings.isDarkMode.value,
                        onCheckedChange = { AppSettings.isDarkMode.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ChimeGreen, checkedTrackColor = ChimeForestGreen)
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // Notifications Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Push Notifications", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Get notified when issues resolve", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = AppSettings.notificationsEnabled.value,
                        onCheckedChange = { AppSettings.notificationsEnabled.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ChimeGreen, checkedTrackColor = ChimeForestGreen)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Offline Database Sync Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Offline Database Sync", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Sync reports for offline access", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = AppSettings.offlineSyncEnabled.value,
                        onCheckedChange = { AppSettings.offlineSyncEnabled.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ChimeGreen, checkedTrackColor = ChimeForestGreen)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Support and Contact Mayor links
        Text(
            text = "Municipality Contacts",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ContactRow(title = "Contact Mayor's Helpdesk", icon = Icons.Default.Call, number = "+1 (800) 555-0199")
                Divider()
                ContactRow(title = "General City Council", icon = Icons.Default.Email, number = "support@smartcivic.gov")
                Divider()
                ContactRow(title = "Emergency Civic Dispatch", icon = Icons.Default.Warning, number = "911 (Civic Hotline)")
            }
        }
        
        if (currentUser != null) {
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ContactRow(title: String, icon: ImageVector, number: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = ChimeForestGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text(text = number, fontSize = 12.sp, color = ChimeForestGreen, fontWeight = FontWeight.Bold)
    }
}
