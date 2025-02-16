@file:OptIn(ExperimentalMaterialApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.wavehitech.aptracker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Embedded
import androidx.room.Relation
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.window.Dialog
import android.graphics.BitmapFactory
import android.util.Log
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort


// Data models
data class AccessPoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String, // e.g., "AP01"
    // Use an observable state list so that changes trigger recomposition
    val pictures: SnapshotStateList<String> = mutableStateListOf()
)

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var accessPoints: MutableList<AccessPoint> = mutableListOf()
)

data class ProjectWithAccessPoints(
    @Embedded val project: ProjectEntity, // Project details
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val accessPoints: List<AccessPointEntity> // Related access points
)



// In-memory store
object ProjectStore {
    val projects = mutableStateListOf<Project>()
}

//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            MaterialTheme {
//                MainNavHost()
//            }
//        }
//    }
//}
// MainActivity: entry point of your app.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ModernMainNavHost()
        }
    }
}


@Composable
fun MyApp() {
    MaterialTheme {
        // Setup Navigation Host
        ModernMainNavHost()
    }
}

//@Composable
//fun MainNavHost() {
//    val navController = rememberNavController()
//    NavHost(navController = navController, startDestination = "projectList") {
//        composable("projectList") {
//            ProjectListScreen(navController)
//        }
//        composable("newProject") {
//            NewProjectScreen(navController)
//        }
//        composable("projectDetail/{projectId}", arguments = listOf(
//            navArgument("projectId") { type = NavType.StringType }
//        )) { backStackEntry ->
//            val projectId = backStackEntry.arguments?.getString("projectId")
//            val project = ProjectStore.projects.find { it.id == projectId }
//            project?.let { ProjectDetailScreen(it, navController) }
//        }
//        composable("apDetail/{projectId}/{apId}", arguments = listOf(
//            navArgument("projectId") { type = NavType.StringType },
//            navArgument("apId") { type = NavType.StringType }
//        )) { backStackEntry ->
//            val projectId = backStackEntry.arguments?.getString("projectId")
//            val apId = backStackEntry.arguments?.getString("apId")
//            val project = ProjectStore.projects.find { it.id == projectId }
//            val ap = project?.accessPoints?.find { it.id == apId }
//            if (ap != null) {
//                APDetailScreen(project, ap, navController)
//            }
//        }
//    }
//}


//@Composable
//fun MainNavHost() {
//    val navController = rememberNavController()
//    NavHost(navController = navController, startDestination = "projectList") {
//        // The "projectList" route shows our ProjectListScreen.
//        composable("projectList") {
//            // Obtain the ViewModel using the factory (defined in your ProjectViewModel.kt file)
//            val viewModel: ProjectViewModel = viewModel(
//                factory = ProjectViewModelFactory(LocalContext.current)
//            )
//            ProjectListScreen(viewModel = viewModel, navController = navController)
//        }
//        // You can add additional routes here (e.g., "newProject", "projectDetail/{projectId}", etc.)
//    }
//}

@Composable
fun ModernMainNavHost() {
    val navController = rememberNavController()
    val viewModel: ProjectViewModel =
        viewModel(factory = ProjectViewModelFactory(LocalContext.current))

    NavHost(navController = navController, startDestination = "projectList") {
        composable("projectList") {
            ModernProjectListScreen(viewModel = viewModel, navController = navController)
        }
        composable("newProject") {
            ModernNewProjectScreen(viewModel = viewModel, navController = navController)
        }
        composable(
            "projectDetail/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val projectEntity = viewModel.projects.find { it.id == projectId }
            projectEntity?.let { project ->
                val accessPoints by viewModel.getAccessPointsForProjectFlow(project.id)
                    .collectAsState(initial = emptyList())
                ModernProjectDetailScreen(
                    projectWithAP = ProjectWithAccessPoints(project, accessPoints),
                    navController = navController
                )
            }
        }
        composable(
            "apDetail/{projectId}/{apId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("apId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val apId = backStackEntry.arguments?.getString("apId") ?: return@composable
            val projectEntity = viewModel.projects.find { it.id == projectId }
            if (projectEntity != null) {
                val accessPoints by viewModel.getAccessPointsForProjectFlow(projectId)
                    .collectAsState(initial = emptyList())
                val apEntity = accessPoints.find { it.id == apId }
                if (apEntity != null) {
                    ModernAPDetailScreen(
                        projectWithAP = ProjectWithAccessPoints(projectEntity, accessPoints),
                        accessPoint = apEntity,
                        navController = navController,
                        viewModel = viewModel
                    )
                } else {
                    Text("Access Point not found", modifier = Modifier.fillMaxSize(), style = MaterialTheme.typography.h6)
                }
            } else {
                Text("Project not found", modifier = Modifier.fillMaxSize(), style = MaterialTheme.typography.h6)
            }
        }
    }
}







//@Composable
//fun ProjectListScreen(navController: NavHostController) {
//    Scaffold(
//        topBar = { TopAppBar(title = { Text("Projects") }) },
//        floatingActionButton = {
//            FloatingActionButton(onClick = { navController.navigate("newProject") }) {
//                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Project")
//            }
//        }
//    ) { paddingValues ->
//        LazyColumn(contentPadding = paddingValues) {
//            items(ProjectStore.projects) { project ->
//                ListItem(
//                    text = { Text(project.name) },
//                    secondaryText = { Text("APs: ${project.accessPoints.size}") },
//                    modifier = Modifier.clickable {
//                        navController.navigate("projectDetail/${project.id}")
//                    }
//                )
//                Divider()
//            }
//        }
//    }
//}


//@Composable
//fun ProjectListScreen(viewModel: ProjectViewModel, navController: androidx.navigation.NavHostController) {
//    // Use the projects list from the ViewModel, which is loaded from your persistent Room database.
//    Scaffold(
//        topBar = { TopAppBar(title = { Text("Projects") }) },
//        floatingActionButton = {
//            FloatingActionButton(onClick = {
//                // Navigate to a "newProject" route if implemented.
//                // navController.navigate("newProject")
//            }) {
//                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Project")
//            }
//        }
//    ) { paddingValues ->
//        LazyColumn(contentPadding = paddingValues) {
//            items(viewModel.projects) { project ->
//                ListItem(
//                    text = { Text(project.name) },
//                    secondaryText = { Text("APs: (update your UI as needed)") },
//                    modifier = Modifier.clickable {
//                        // Navigate to a project detail screen if implemented.
//                        // navController.navigate("projectDetail/${project.id}")
//                    }
//                )
//                Divider()
//            }
//        }
//    }
//}

@Composable
fun ModernProjectListScreen(
    viewModel: ProjectViewModel,
    navController: NavHostController
) {
    // State for multi-select deletion.
    var selectionMode by remember { mutableStateOf(false) }
    val selectedProjects = remember { mutableStateListOf<ProjectEntity>() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // BackHandler: when selection mode is active, clear selection instead of navigating back.
    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedProjects.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectionMode) "${selectedProjects.size} Selected" else "Projects",
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedProjects.clear()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit selection")
                        }
                    }
                },
                actions = {
                    if (selectionMode && selectedProjects.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = { navController.navigate("newProject") }) {
                    Icon(Icons.Default.Add, contentDescription = "New Project")
                }
            }
        },
        content = { paddingValues ->
            if (viewModel.projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No projects available. Tap + to add.", style = MaterialTheme.typography.body1)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(viewModel.projects) { project ->
                        ProjectListItem(
                            project = project,
                            selectionMode = selectionMode,
                            isSelected = selectedProjects.contains(project),
                            onClick = {
                                if (selectionMode) {
                                    if (selectedProjects.contains(project)) {
                                        selectedProjects.remove(project)
                                        if (selectedProjects.isEmpty()) selectionMode = false
                                    } else {
                                        selectedProjects.add(project)
                                    }
                                } else {
                                    navController.navigate("projectDetail/${project.id}")
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) selectionMode = true
                                if (!selectedProjects.contains(project)) {
                                    selectedProjects.add(project)
                                }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Projects") },
            text = { Text("Are you sure you want to delete ${selectedProjects.size} project(s)?") },
            confirmButton = {
                Button(onClick = {
                    selectedProjects.forEach { project ->
                        viewModel.deleteProjectWithAccessPoints(project.id)
                    }
                    selectedProjects.clear()
                    selectionMode = false
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProjectListItem(
    project: ProjectEntity,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = project.name,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.weight(1f)
        )
    }
}



//@Composable
//fun NewProjectScreen(navController: NavHostController) {
//    var projectName by remember { mutableStateOf(TextFieldValue("")) }
//    var expectedAPsText by remember { mutableStateOf(TextFieldValue("0")) }
//
//    Scaffold(topBar = { TopAppBar(title = { Text("New Project") }) }) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .padding(paddingValues)
//                .padding(16.dp)
//        ) {
//            OutlinedTextField(
//                value = projectName,
//                onValueChange = { projectName = it },
//                label = { Text("Project Name") },
//                modifier = Modifier.fillMaxWidth()
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            OutlinedTextField(
//                value = expectedAPsText,
//                onValueChange = { expectedAPsText = it },
//                label = { Text("Expected Number of APs") },
//                modifier = Modifier.fillMaxWidth()
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Button(
//                onClick = {
//                    val expectedAPs = expectedAPsText.text.toIntOrNull() ?: 0
//                    val newProject = Project(name = projectName.text)
//                    for (i in 1..expectedAPs) {
//                        val apName = "AP" + i.toString().padStart(2, '0')
//                        newProject.accessPoints.add(AccessPoint(name = apName))
//                    }
//                    ProjectStore.projects.add(newProject)
//                    navController.popBackStack()
//                },
//                modifier = Modifier.align(Alignment.End)
//            ) {
//                Text("Create Project")
//            }
//        }
//    }
//}

@Composable
fun ModernNewProjectScreen(
    viewModel: ProjectViewModel,
    navController: NavHostController
) {
    var projectName by remember { mutableStateOf(TextFieldValue("")) }
    var expectedAPsText by remember { mutableStateOf(TextFieldValue("0")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Project", style = MaterialTheme.typography.h6) },
                elevation = 4.dp
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = expectedAPsText,
                    onValueChange = { expectedAPsText = it },
                    label = { Text("Expected Number of APs") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        val expectedAPs = expectedAPsText.text.toIntOrNull() ?: 0
                        val newProject = ProjectEntity(
                            id = UUID.randomUUID().toString(),
                            name = projectName.text
                        )
                        val accessPoints = (1..expectedAPs).map { i ->
                            val apName = "AP" + i.toString().padStart(2, '0')
                            AccessPointEntity(
                                id = UUID.randomUUID().toString(),
                                projectId = newProject.id,
                                name = apName,
                                pictures = emptyList()
                            )
                        }
                        viewModel.addProjectWithAccessPoints(newProject, accessPoints)
                        navController.popBackStack()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Create Project")
                }
            }
        }
    )
}



//@Composable
//fun ProjectDetailScreen(project: Project, navController: NavHostController) {
//    val context = LocalContext.current
//
//    var sortOption by remember { mutableStateOf("Alphabetical") }
//    var showAddAPDialog by remember { mutableStateOf(false) }
//    var showExportDialog by remember { mutableStateOf(false) }
//    val sortedAPs = when (sortOption) {
//        "Alphabetical" -> project.accessPoints.sortedBy { it.name }
//        "Number of Pictures" -> project.accessPoints.sortedByDescending { it.pictures.size }
//        "APs without Pictures" -> project.accessPoints.sortedBy { if (it.pictures.isEmpty()) 0 else 1 }
//        else -> project.accessPoints
//    }
//
//    if (showAddAPDialog) {
//        AddAPDialog(
//            onDismiss = { showAddAPDialog = false },
//            onAdd = { newAPName ->
//                project.accessPoints.add(AccessPoint(name = newAPName))
//                showAddAPDialog = false
//            }
//        )
//    }
//
//    if (showExportDialog) {
//        ExportDialog(
//            onDismiss = { showExportDialog = false },
//            onSave = {
//                val zipFile = exportProjectToZip(context, project)
//                val savedFile = saveZipToDocuments(context, zipFile)
//                Toast.makeText(
//                    context,
//                    "Saved to ${savedFile?.absolutePath ?: "Error saving file"}",
//                    Toast.LENGTH_LONG
//                ).show()
//                showExportDialog = false
//            },
//            onShare = {
//                val zipFile = exportProjectToZip(context, project)
//                shareZipFile(context, zipFile)
//                showExportDialog = false
//            }
//        )
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(project.name) },
//                actions = {
//                    IconButton(onClick = { showExportDialog = true }) {
//                        Icon(imageVector = Icons.Default.Archive, contentDescription = "Export Project")
//                    }
//                    var expanded by remember { mutableStateOf(false) }
//                    Box {
//                        IconButton(onClick = { expanded = true }) {
//                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
//                        }
//                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
//                            DropdownMenuItem(onClick = {
//                                sortOption = "Alphabetical"
//                                expanded = false
//                            }) { Text("Alphabetical") }
//                            DropdownMenuItem(onClick = {
//                                sortOption = "Number of Pictures"
//                                expanded = false
//                            }) { Text("Number of Pictures") }
//                            DropdownMenuItem(onClick = {
//                                sortOption = "APs without Pictures"
//                                expanded = false
//                            }) { Text("APs without Pictures") }
//                        }
//                    }
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(onClick = { showAddAPDialog = true }) {
//                Icon(imageVector = Icons.Default.Add, contentDescription = "Add AP")
//            }
//        }
//    ) { paddingValues ->
//        LazyColumn(contentPadding = paddingValues) {
//            items(sortedAPs) { ap ->
//                ListItem(
//                    text = { Text("${ap.name} (${ap.pictures.size})") },
//                    secondaryText = { Text("Tap to view pictures") },
//                    modifier = Modifier.clickable {
//                        navController.navigate("apDetail/${project.id}/${ap.id}")
//                    }
//                )
//                Divider()
//            }
//        }
//    }
//}

@Composable
fun ModernProjectDetailScreen(
    projectWithAP: ProjectWithAccessPoints,
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: ProjectViewModel = viewModel(factory = ProjectViewModelFactory(context))

    // Collect access points from the database.
    val accessPoints by viewModel.getAccessPointsForProjectFlow(projectWithAP.project.id)
        .collectAsState(initial = projectWithAP.accessPoints)

    // When the list of access points changes, check each for missing images.
    // Since we store only file names, we build a File from context.filesDir.
    LaunchedEffect(accessPoints) {
        accessPoints.forEach { ap ->
            val validImages = ap.pictures.filter { pictureName ->
                val file = File(context.filesDir, pictureName)
                file.exists()
            }
            if (validImages.size != ap.pictures.size) {
                viewModel.updateAccessPoint(ap.copy(pictures = validImages))
            }
        }
    }

    var sortOption by remember { mutableStateOf("Alphabetical") }
    var showAddAPDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var apSelectionMode by remember { mutableStateOf(false) }
    val selectedAPs = remember { mutableStateListOf<AccessPointEntity>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Sorted list of access points based on the selected sort option.
    val sortedAPs = when (sortOption) {
        "Alphabetical" -> accessPoints.sortedWith(compareBy(
            { it.name.takeWhile { char -> !char.isDigit() } },
            { it.name.dropWhile { char -> !char.isDigit() }.toIntOrNull() ?: 0 }
        ))
        "Number of Pictures" -> accessPoints.sortedByDescending { it.pictures.size }
        "APs without Pictures" -> accessPoints.sortedBy { if (it.pictures.isEmpty()) 0 else 1 }
        else -> accessPoints
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (apSelectionMode) "${selectedAPs.size} Selected" else projectWithAP.project.name,
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    if (apSelectionMode) {
                        IconButton(onClick = {
                            apSelectionMode = false
                            selectedAPs.clear()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit selection")
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (apSelectionMode && selectedAPs.isNotEmpty()) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    } else {
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(Icons.Default.Archive, contentDescription = "Export Project")
                        }
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                                DropdownMenuItem(onClick = {
                                    sortOption = "Alphabetical"
                                    dropdownExpanded = false
                                }) { Text("Alphabetical") }
                                DropdownMenuItem(onClick = {
                                    sortOption = "Number of Pictures"
                                    dropdownExpanded = false
                                }) { Text("Number of Pictures") }
                                DropdownMenuItem(onClick = {
                                    sortOption = "APs without Pictures"
                                    dropdownExpanded = false
                                }) { Text("APs without Pictures") }
                            }
                        }
                    }
                },
                elevation = 4.dp
            )
        },
        floatingActionButton = {
            if (!apSelectionMode) {
                FloatingActionButton(onClick = { showAddAPDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add AP")
                }
            }
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(sortedAPs) { ap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .combinedClickable(
                                onClick = {
                                    if (apSelectionMode) {
                                        if (selectedAPs.contains(ap)) {
                                            selectedAPs.remove(ap)
                                            if (selectedAPs.isEmpty()) apSelectionMode = false
                                        } else {
                                            selectedAPs.add(ap)
                                        }
                                    } else {
                                        navController.navigate("apDetail/${projectWithAP.project.id}/${ap.id}")
                                    }
                                },
                                onLongClick = {
                                    if (!apSelectionMode) { apSelectionMode = true }
                                    if (!selectedAPs.contains(ap)) { selectedAPs.add(ap) }
                                }
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (apSelectionMode) {
                            Checkbox(
                                checked = selectedAPs.contains(ap),
                                onCheckedChange = {
                                    if (it) selectedAPs.add(ap) else {
                                        selectedAPs.remove(ap)
                                        if (selectedAPs.isEmpty()) apSelectionMode = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Display AP name and current picture count.
                            Text(
                                text = "${ap.name} (${ap.pictures.size})",
                                style = MaterialTheme.typography.subtitle1
                            )
                            Text(
                                text = "Tap to view pictures",
                                style = MaterialTheme.typography.body2,
                                color = Color.Gray
                            )
                        }
                    }
                    Divider()
                }
            }
        }
    )

    // Confirmation dialog for deleting selected APs.
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${selectedAPs.size} selected AP(s)?") },
            confirmButton = {
                Button(onClick = {
                    selectedAPs.forEach { ap ->
                        viewModel.deleteAccessPoint(ap.id)
                    }
                    selectedAPs.clear()
                    apSelectionMode = false
                    showDeleteConfirmDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Display Add AP dialog.
    if (showAddAPDialog) {
        AddAPDialog(
            projectId = projectWithAP.project.id,
            viewModel = viewModel,
            onDismiss = { showAddAPDialog = false }
        )
    }

    // Export dialog (with pre-export scan/rename logic).
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onSave = { progressState, isZipping, onDismissExport ->
                // For every access point, scan for missing images and rename.
                projectWithAP.accessPoints.forEach { ap ->
                    val renamedImages = renameAPImages(context, ap.name, ap.pictures)
                    if (renamedImages != ap.pictures) {
                        viewModel.updateAccessPoint(ap.copy(pictures = renamedImages))
                    }
                }
                // Then launch export (using your exportProjectToZip logic).
                CoroutineScope(Dispatchers.IO).launch {
                    val zipFile = exportProjectToZip(context, projectWithAP, progressState, isZipping, onDismissExport)
                    withContext(Dispatchers.Main) {
                        // e.g., update UI or show a toast.
                    }
                }
            },
            onShare = { progressState, isZipping, onDismissExport ->
                projectWithAP.accessPoints.forEach { ap ->
                    val renamedImages = renameAPImages(context, ap.name, ap.pictures)
                    if (renamedImages != ap.pictures) {
                        viewModel.updateAccessPoint(ap.copy(pictures = renamedImages))
                    }
                }
                CoroutineScope(Dispatchers.IO).launch {
                    val zipFile = exportProjectToZip(context, projectWithAP, progressState, isZipping, onDismissExport)
                    withContext(Dispatchers.Main) {
                        shareZipFile(context, zipFile)
                    }
                }
            },
            warningMessage = accessPoints.filter { it.pictures.size < 2 }
                .joinToString { it.name }
                .takeIf { it.isNotEmpty() }
                ?.let { "Warning: The following AP(s) have fewer than 2 pictures: $it" }
        )
    }

    // Intercept system back button to clear selection if active.
    BackHandler(enabled = apSelectionMode) {
        apSelectionMode = false
        selectedAPs.clear()
    }
}




@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
//
//@Composable
//fun APDetailScreen(project: Project, ap: AccessPoint, navController: NavHostController) {
//    val context = LocalContext.current
//    var showImageDialog by remember { mutableStateOf<String?>(null) }
//    var selectionMode by remember { mutableStateOf(false) }
//    val selectedPictures = remember { mutableStateListOf<String>() }
//    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
//
//    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
//        if (bitmap != null) {
//            val pictureCount = ap.pictures.size + 1
//            val fileName = "${ap.name}-$pictureCount.jpg"
//            val filePath = saveBitmapToFile(context, bitmap, fileName)
//            ap.pictures.add(filePath)
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("${ap.name} Pictures") },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(onClick = { launcher.launch(null) }) {
//                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Picture")
//            }
//        }
//    ) { paddingValues ->
//        Column {
//            if (ap.pictures.isEmpty()) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(paddingValues),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("No pictures available. Tap + to add.")
//                }
//            } else {
//                LazyVerticalGrid(
//                    columns = GridCells.Fixed(3),
//                    modifier = Modifier
//                        .padding(paddingValues)
//                        .weight(1f),
//                    contentPadding = PaddingValues(4.dp)
//                ) {
//                    items(ap.pictures) { picturePath ->
//                        Box(
//                            modifier = Modifier
//                                .padding(4.dp)
//                                .aspectRatio(1f)
//                                .combinedClickable(
//                                    onClick = {
//                                        if (selectionMode) {
//                                            if (selectedPictures.contains(picturePath)) {
//                                                selectedPictures.remove(picturePath)
//                                                if (selectedPictures.isEmpty()) selectionMode = false
//                                            } else {
//                                                selectedPictures.add(picturePath)
//                                            }
//                                        } else {
//                                            showImageDialog = picturePath
//                                        }
//                                    },
//                                    onLongClick = {
//                                        if (!selectionMode) {
//                                            selectionMode = true
//                                        }
//                                        if (!selectedPictures.contains(picturePath)) {
//                                            selectedPictures.add(picturePath)
//                                        }
//                                    }
//                                )
//                        ) {
//                            AsyncImage(
//                                model = File(picturePath),
//                                contentDescription = null,
//                                modifier = Modifier.fillMaxSize()
//                            )
//                            if (selectionMode && selectedPictures.contains(picturePath)) {
//                                Box(
//                                    modifier = Modifier.fillMaxSize(),
//                                    contentAlignment = Alignment.TopEnd
//                                ) {
//                                    Icon(
//                                        imageVector = Icons.Default.Check,
//                                        contentDescription = "Selected",
//                                        tint = MaterialTheme.colors.primary,
//                                        modifier = Modifier.size(24.dp)
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//            if (selectionMode && selectedPictures.isNotEmpty()) {
//                Button(
//                    onClick = { showDeleteConfirmDialog = true },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(8.dp)
//                ) {
//                    Text("Delete Selected (${selectedPictures.size})")
//                }
//            }
//        }
//    }
//
//    if (showDeleteConfirmDialog) {
//        AlertDialog(
//            onDismissRequest = { showDeleteConfirmDialog = false },
//            title = { Text("Confirm Deletion") },
//            text = { Text("Are you sure you want to delete ${selectedPictures.size} selected picture(s)?") },
//            confirmButton = {
//                Button(onClick = {
//                    ap.pictures.removeAll(selectedPictures)
//                    selectedPictures.clear()
//                    selectionMode = false
//                    showDeleteConfirmDialog = false
//                }) {
//                    Text("Delete")
//                }
//            },
//            dismissButton = {
//                Button(onClick = { showDeleteConfirmDialog = false }) {
//                    Text("Cancel")
//                }
//            }
//        )
//    }
//
//    if (showImageDialog != null) {
//        ImageDialog(
//            imagePath = showImageDialog!!,
//            onDismiss = { showImageDialog = null },
//            onDelete = {
//                ap.pictures.remove(showImageDialog)
//                showImageDialog = null
//            }
//        )
//    }
//}

// Define a Saver for Uri
val UriNullableSaver = Saver<Uri?, String>(
    save = { it?.toString() ?: "" },
    restore = { if (it.isEmpty()) null else Uri.parse(it) }
)

@Composable
fun ModernAPDetailScreen(
    projectWithAP: ProjectWithAccessPoints,
    accessPoint: AccessPointEntity,
    navController: NavHostController,
    viewModel: ProjectViewModel
) {
    val context = LocalContext.current

    // The stored images are file names (e.g. "AP01-1.jpg") in filesDir.
    val fullSizeImagesState = rememberSaveable(stateSaver = listSaver(
        save = { it },
        restore = { it }
    )) { mutableStateOf(accessPoint.pictures) }

    // A state variable to hold the file name for the new image.
    var currentFileName by remember { mutableStateOf("") }

    // On entering the screen, scan for missing files and rename the remaining ones.
    LaunchedEffect(accessPoint.id) {
        // Build a list of valid images from filesDir.
        val validImages = fullSizeImagesState.value.filter { pictureName ->
            val file = File(context.filesDir, pictureName)
            file.exists()
        }
        // Rename valid images so numbering starts at 1.
        val renamedImages = renameAPImages(context, accessPoint.name, validImages)
        if (renamedImages != fullSizeImagesState.value) {
            fullSizeImagesState.value = renamedImages
            viewModel.updateAccessPoint(accessPoint.copy(pictures = renamedImages))
        }
    }

    // State for selection mode and selected pictures.
    var selectionMode by remember { mutableStateOf(false) }
    val selectedPictures = remember { mutableStateListOf<String>() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var imageUri: Uri? by rememberSaveable(stateSaver = UriNullableSaver) { mutableStateOf(null) }

    // When in selection mode, override back button to clear selection.
    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedPictures.clear()
    }

    // Launcher for taking a picture.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            // The new file's name is already stored in currentFileName.
            fullSizeImagesState.value = fullSizeImagesState.value + currentFileName
            viewModel.updateAccessPoint(accessPoint.copy(pictures = fullSizeImagesState.value))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${accessPoint.name} Pictures", style = MaterialTheme.typography.h6) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectionMode && selectedPictures.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    }
                },
                elevation = 4.dp
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Create a new file name based on the current count.
                currentFileName = "${accessPoint.name}-${fullSizeImagesState.value.size + 1}.jpg"
                val file = File(context.filesDir, currentFileName)
                // Get a content URI via FileProvider.
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.wavehitech.aptracker.provider",
                    file
                )
                imageUri = uri
                cameraLauncher.launch(uri)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Picture")
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (fullSizeImagesState.value.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No pictures available. Tap + to add.", style = MaterialTheme.typography.body1)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(fullSizeImagesState.value) { pictureName ->
                            val file = File(context.filesDir, pictureName)
                            // Use FileProvider to get a content URI for viewing.
                            val imageUri = FileProvider.getUriForFile(
                                context,
                                "com.wavehitech.aptracker.provider",
                                file
                            )
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .aspectRatio(1f)
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionMode) {
                                                if (selectedPictures.contains(pictureName)) {
                                                    selectedPictures.remove(pictureName)
                                                    if (selectedPictures.isEmpty()) selectionMode = false
                                                } else {
                                                    selectedPictures.add(pictureName)
                                                }
                                            } else {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(imageUri, "image/*")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                            }
                                        },
                                        onLongClick = {
                                            if (!selectionMode) selectionMode = true
                                            if (!selectedPictures.contains(pictureName)) {
                                                selectedPictures.add(pictureName)
                                            }
                                        }
                                    )
                            ) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (selectionMode && selectedPictures.contains(pictureName)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = {
                val count = if (selectionMode) selectedPictures.size else 1
                Text("Are you sure you want to delete $count image(s)?")
            },
            confirmButton = {
                Button(onClick = {
                    if (selectionMode) {
                        // Delete each selected file.
                        selectedPictures.forEach { pictureName ->
                            val file = File(context.filesDir, pictureName)
                            if (file.exists()) file.delete()
                        }
                        // Remove deleted images from the list.
                        fullSizeImagesState.value = fullSizeImagesState.value - selectedPictures
                        // Rename remaining images so numbering starts at 1.
                        val renamedImages = renameAPImages(context, accessPoint.name, fullSizeImagesState.value)
                        fullSizeImagesState.value = renamedImages
                        viewModel.updateAccessPoint(accessPoint.copy(pictures = renamedImages))
                        selectedPictures.clear()
                        selectionMode = false
                    }
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



// Save image to scoped storage using MediaStore (no external storage permissions)
fun saveImageToScopedStorage(context: Context, uri: Uri, accessPointName: String, pictureCount: Int): Uri? {
    val formattedFileName = "${accessPointName}-${pictureCount + 1}.jpg" // Ensure correct naming

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, formattedFileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/YourAppName")
        }
    }

    val imageUri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    imageUri?.let {
        context.contentResolver.openOutputStream(it)?.use { outputStream ->
            val orientation = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ExifInterface(inputStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            if (bitmap != null) {
                val rotatedBitmap = rotateBitmapIfNeeded(bitmap, orientation)
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            } else {
                Log.e("saveImageToScopedStorage", "Failed to decode bitmap from input stream.")
            }
        }
    }
    return imageUri
}



fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    return if (degrees != 0f) {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}


@Composable
fun ImageDialog(imagePath: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this picture?") },
            confirmButton = {
                Button(onClick = {
                    onDelete()
                    showConfirmDelete = false
                }) { Text("Delete") }
            },
            dismissButton = {
                Button(onClick = { showConfirmDelete = false }) { Text("Cancel") }
            }
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        buttons = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                    Button(onClick = { showConfirmDelete = true }) {
                        Text("Delete")
                    }
                }
            }
        },
        text = {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}


fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): String {
    val file = File(context.filesDir, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
    }
    return file.absolutePath
}

fun exportProjectToZip(
    context: Context,
    projectWithAP: ProjectWithAccessPoints,
    progressState: MutableState<Int>,
    isZipping: MutableState<Boolean>,
    onDismiss: () -> Unit
): File {
    // Create a zip file in the app's cache directory.
    val zipFile = File(context.cacheDir, "${projectWithAP.project.name}.zip")
    // Count the total number of images across all access points.
    val totalFiles = projectWithAP.accessPoints.sumOf { it.pictures.size }
    var processedFiles = 0

    ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
        projectWithAP.accessPoints.forEach { ap ->
            ap.pictures.forEachIndexed { index, pictureName ->
                // Build the file from internal storage using context.filesDir.
                val file = File(context.filesDir, pictureName)
                if (file.exists()) {
                    // Use the AP name and new index to create an entry name.
                    val formattedFileName = "${ap.name}-${index + 1}.jpg"
                    val entry = ZipEntry(formattedFileName)
                    zos.putNextEntry(entry)
                    file.inputStream().copyTo(zos)
                    zos.closeEntry()

                    processedFiles++
                    progressState.value = ((processedFiles.toFloat() / totalFiles) * 100).toInt()
                }
            }
        }
    }

    // Once done, switch back to Main dispatcher.
    CoroutineScope(Dispatchers.Main).launch {
        isZipping.value = false
        onDismiss() // close the export dialog.
    }
    return zipFile
}



fun getFileFromUri(context: Context, uri: Uri): File? {
    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
    val cursor = context.contentResolver.query(uri, filePathColumn, null, null, null)

    cursor?.use {
        if (it.moveToFirst()) {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val filePath = it.getString(columnIndex)
            return if (filePath != null) File(filePath) else null
        }
    }
    return null
}


@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onSave: (MutableState<Int>, MutableState<Boolean>, () -> Unit) -> Unit,
    onShare: (MutableState<Int>, MutableState<Boolean>, () -> Unit) -> Unit,
    warningMessage: String? = null
) {
    val isZipping = remember { mutableStateOf(false) }
    val progress = remember { mutableStateOf(0) }
    Dialog(onDismissRequest = { if (!isZipping.value) onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isZipping.value) "Zipping Files..." else "Export Project",
                    style = MaterialTheme.typography.h6
                )
                if (isZipping.value) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(progress = progress.value / 100f, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }
                warningMessage?.let {
                    if (!isZipping.value) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.height(150.dp).verticalScroll(rememberScrollState())) {
                            Text(text = it, color = Color.Red, style = MaterialTheme.typography.body2)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (!isZipping.value) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            isZipping.value = true
                            CoroutineScope(Dispatchers.IO).launch {
                                onShare(progress, isZipping, onDismiss)
                            }
                        }, modifier = Modifier.weight(1f)) { Text("Share") }
                        Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        isZipping.value = true
                        CoroutineScope(Dispatchers.IO).launch {
                            onSave(progress, isZipping, onDismiss)
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save to Documents") }
                }
            }
        }
    }
}

@Composable
fun AddAPDialog(
    projectId: String,
    viewModel: ProjectViewModel,
    onDismiss: () -> Unit
) {
    var apName by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Access Point") },
        text = {
            OutlinedTextField(
                value = apName,
                onValueChange = { apName = it },
                label = { Text("AP Name (e.g., AP05)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val name = apName.text.trim()
                if (name.isNotEmpty()) {
                    val newAP = AccessPointEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = projectId,
                        name = name,
                        pictures = emptyList()
                    )
                    viewModel.addAccessPoint(newAP)
                    onDismiss()
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}




fun saveZipToDocuments(context: Context, zipFile: File): File? {
    return try {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
        val destinationFile = File(documentsDir, zipFile.name)
        zipFile.copyTo(destinationFile, overwrite = true)
        destinationFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun shareZipFile(context: Context, zipFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share ZIP file"))
}

/**
 * Renames all image files for the given access point so that numbering starts at 1.
 * @param context The Context.
 * @param accessPointName The name of the access point (e.g., "AP01").
 * @param images A list of file names (not absolute paths) stored in filesDir.
 * @return A new list of file names after renaming.
 */
fun renameAPImages(context: Context, accessPointName: String, images: List<String>): List<String> {
    return images.mapIndexed { index, fileName ->
        val newIndex = index + 1
        val newFileName = "$accessPointName-$newIndex.jpg"
        val oldFile = File(context.filesDir, fileName)
        val newFile = File(context.filesDir, newFileName)
        if (oldFile.exists() && oldFile.name != newFileName) {
            oldFile.renameTo(newFile)
        }
        newFile.name  // Return just the file name.
    }
}
