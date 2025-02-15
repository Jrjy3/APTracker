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
            MainNavHost()
        }
    }
}


@Composable
fun MyApp() {
    MaterialTheme {
        // Setup Navigation Host
        MainNavHost()
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
fun MainNavHost() {
    val navController = rememberNavController()
    val viewModel: ProjectViewModel = viewModel(factory = ProjectViewModelFactory(LocalContext.current))

    NavHost(navController = navController, startDestination = "projectList") {
        composable("projectList") {
            ProjectListScreen(viewModel = viewModel, navController = navController)
        }
        composable("newProject") {
            NewProjectScreen(viewModel = viewModel, navController = navController)
        }
        composable(
            "projectDetail/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")
            val projectEntity = viewModel.projects.find { it.id == projectId }
            projectEntity?.let { project ->
                val accessPoints by viewModel.getAccessPointsForProjectFlow(projectId!!)
                    .collectAsState(initial = emptyList<AccessPointEntity>())
                val projectWithAP = ProjectWithAccessPoints(project, accessPoints)
                ProjectDetailScreen(projectWithAP, navController)
            }
        }
        composable(
            "apDetail/{projectId}/{apId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("apId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")
            val apId = backStackEntry.arguments?.getString("apId")
            val projectEntity = viewModel.projects.find { it.id == projectId }
            if (projectEntity != null) {
                val accessPoints by viewModel.getAccessPointsForProjectFlow(projectId!!)
                    .collectAsState(initial = emptyList())
                val apEntity = accessPoints.find { it.id == apId }
                if (apEntity != null) {
                    val projectWithAP = ProjectWithAccessPoints(projectEntity, accessPoints)
                    APDetailScreen(projectWithAP = projectWithAP, accessPoint = apEntity, navController = navController, viewModel = viewModel)
                } else {
                    Text("AP not found")
                }
            } else {
                Text("Project not found")
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
fun ProjectListScreen(
    viewModel: ProjectViewModel,
    navController: NavHostController
) {
    var selectionMode by remember { mutableStateOf(false) }
    val selectedProjects = remember { mutableStateListOf<ProjectEntity>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Text("${selectedProjects.size} selected", style = MaterialTheme.typography.h6)
                    } else {
                        Text("Projects", style = MaterialTheme.typography.h6)
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedProjects.clear()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Exit selection mode"
                            )
                        }
                    }
                },
                actions = {
                    if (selectionMode && selectedProjects.isNotEmpty()) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected projects"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = { navController.navigate("newProject") }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Project")
                }
            }
        }
    ) { paddingValues ->
        // Check if there are any projects.
        if (viewModel.projects.isEmpty()) {
            // Show a friendly message when no projects are available.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No projects available. Tap + to add.")
            }
        } else {
            LazyColumn(contentPadding = paddingValues) {
                items(viewModel.projects) { project ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .combinedClickable(
                                onClick = {
                                    if (selectionMode) {
                                        if (selectedProjects.contains(project)) {
                                            selectedProjects.remove(project)
                                            if (selectedProjects.isEmpty()) {
                                                selectionMode = false
                                            }
                                        } else {
                                            selectedProjects.add(project)
                                        }
                                    } else {
                                        navController.navigate("projectDetail/${project.id}")
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        selectionMode = true
                                    }
                                    if (!selectedProjects.contains(project)) {
                                        selectedProjects.add(project)
                                    }
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectionMode) {
                                Checkbox(
                                    checked = selectedProjects.contains(project),
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            selectedProjects.add(project)
                                        } else {
                                            selectedProjects.remove(project)
                                            if (selectedProjects.isEmpty()) {
                                                selectionMode = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    Divider()
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Projects") },
            text = { Text("Are you sure you want to delete ${selectedProjects.size} selected project(s)?") },
            confirmButton = {
                Button(onClick = {
                    selectedProjects.forEach { project ->
                        viewModel.deleteProjectWithAccessPoints(project.id)
                    }
                    selectedProjects.clear()
                    selectionMode = false
                    showDeleteConfirmation = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
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
fun NewProjectScreen(viewModel: ProjectViewModel, navController: NavHostController) {
    var projectName by remember { mutableStateOf(TextFieldValue("")) }
    var expectedAPsText by remember { mutableStateOf(TextFieldValue("0")) }

    Scaffold(topBar = { TopAppBar(title = { Text("New Project") }) }) { paddingValues ->
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
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = expectedAPsText,
                onValueChange = { expectedAPsText = it },
                label = { Text("Expected Number of APs") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                            pictures = emptyList() // Start with no pictures.
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
fun ProjectDetailScreen(
    projectWithAP: ProjectWithAccessPoints,
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: ProjectViewModel = viewModel(factory = ProjectViewModelFactory(LocalContext.current))
    // Collect APs from the database.
    val accessPointsState by viewModel.getAccessPointsForProjectFlow(projectWithAP.project.id)
        .collectAsState(initial = projectWithAP.accessPoints)

    var sortOption by remember { mutableStateOf("Alphabetical") }
    var showAddAPDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteAPConfirm by remember { mutableStateOf(false) }
    // For AP selection deletion mode
    var apSelectionMode by remember { mutableStateOf(false) }
    val selectedAPs = remember { mutableStateListOf<AccessPointEntity>() }

    // Sort APs as before
    val sortedAPs = when (sortOption) {
        "Alphabetical" -> accessPointsState.sortedWith(compareBy(
            // Compare by the non-digit prefix
            { it.name.takeWhile { c -> !c.isDigit() } },
            // Then compare by the numeric part (if present, default to 0)
            { it.name.dropWhile { c -> !c.isDigit() }.toIntOrNull() ?: 0 }
        ))
        "Number of Pictures" -> accessPointsState.sortedByDescending { it.pictures.size }
        "APs without Pictures" -> accessPointsState.sortedBy { if (it.pictures.isEmpty()) 0 else 1 }
        else -> accessPointsState
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (apSelectionMode) {
                        Text("${selectedAPs.size} selected", style = MaterialTheme.typography.h6)
                    } else {
                        Text(projectWithAP.project.name, style = MaterialTheme.typography.h6)
                    }
                },
                navigationIcon = {
                    if (apSelectionMode) {
                        IconButton(onClick = {
                            apSelectionMode = false
                            selectedAPs.clear()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Exit selection mode"
                            )
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (apSelectionMode) {
                        // Show delete button if one or more APs are selected
                        if (selectedAPs.isNotEmpty()) {
                            IconButton(onClick = { showDeleteAPConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete selected APs"
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Export Project"
                            )
                        }
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort"
                                )
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(onClick = {
                                    sortOption = "Alphabetical"
                                    expanded = false
                                }) { Text("Alphabetical") }
                                DropdownMenuItem(onClick = {
                                    sortOption = "Number of Pictures"
                                    expanded = false
                                }) { Text("Number of Pictures") }
                                DropdownMenuItem(onClick = {
                                    sortOption = "APs without Pictures"
                                    expanded = false
                                }) { Text("APs without Pictures") }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!apSelectionMode) {
                FloatingActionButton(onClick = { showAddAPDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add AP")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            items(sortedAPs) { ap ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)   // increased height for two lines
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
                                if (!apSelectionMode) {
                                    apSelectionMode = true
                                }
                                if (!selectedAPs.contains(ap)) {
                                    selectedAPs.add(ap)
                                }
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (apSelectionMode) {
                        Checkbox(
                            checked = selectedAPs.contains(ap),
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    selectedAPs.add(ap)
                                } else {
                                    selectedAPs.remove(ap)
                                    if (selectedAPs.isEmpty()) apSelectionMode = false
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        // When not in selection mode, add extra left padding.
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = "${ap.name} (${ap.pictures.size})",
                            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Normal)
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

    if (showDeleteAPConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAPConfirm = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${selectedAPs.size} AP(s) and all associated pictures?") },
            confirmButton = {
                Button(onClick = {
                    // Loop through the selected APs
                    selectedAPs.forEach { ap ->
                        // Delete each picture from storage
                        ap.pictures.forEach { picturePath ->
                            val file = File(context.filesDir, picturePath)
                            if (file.exists()) file.delete()
                        }
                        // Delete the AP from the database (assume viewModel.deleteAccessPoint exists)
                        viewModel.deleteAccessPoint(ap.id)
                    }
                    selectedAPs.clear()
                    apSelectionMode = false
                    showDeleteAPConfirm = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteAPConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddAPDialog) {
        AddAPDialog(
            projectId = projectWithAP.project.id,
            viewModel = viewModel,
            onDismiss = { showAddAPDialog = false }
        )
    }

    if (showExportDialog) {
        // Existing ExportDialog code remains unchanged
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onSave = { progressState, isZipping, onDismissExport ->
                CoroutineScope(Dispatchers.IO).launch {
                    val zipFile = exportProjectToZip(context, projectWithAP, progressState, isZipping, onDismissExport)
                    val savedFile = saveZipToDocuments(context, zipFile)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Saved to ${savedFile?.absolutePath ?: "Error saving file"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            onShare = { progressState, isZipping, onDismissExport ->
                CoroutineScope(Dispatchers.IO).launch {
                    val zipFile = exportProjectToZip(context, projectWithAP, progressState, isZipping, onDismissExport)
                    withContext(Dispatchers.Main) {
                        shareZipFile(context, zipFile)
                    }
                }
            },
            warningMessage = accessPointsState.filter { it.pictures.size < 2 }
                .joinToString { it.name }
                .let { if (it.isNotEmpty()) "Warning: The following AP(s) have fewer than 2 pictures: $it" else null }
        )
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
fun APDetailScreen(
    projectWithAP: ProjectWithAccessPoints,
    accessPoint: AccessPointEntity,
    navController: NavHostController,
    viewModel: ProjectViewModel
) {
    val context = LocalContext.current
    // Use a mutableStateOf holding a List<String> so that it can be saved/restored.
    val fullSizeImagesState = rememberSaveable(stateSaver = listSaver(
        save = { list: List<String> -> list },
        restore = { it }
    )) { mutableStateOf(listOf<String>()) }

    // Initialize the list when the access point is loaded.
    LaunchedEffect(accessPoint) {
        if (fullSizeImagesState.value.isEmpty()) {
            fullSizeImagesState.value = accessPoint.pictures
        }
    }

    var selectionMode by remember { mutableStateOf(false) }
    // We'll keep selected pictures in a mutableStateListOf since this is transient.
    val selectedPictures = remember { mutableStateListOf<String>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var imageUri: Uri? by rememberSaveable(stateSaver = UriNullableSaver) { mutableStateOf(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && imageUri != null) {
            val newUri = imageUri!!
            Log.d("APDetailScreen", "Captured image URI: $newUri")

            // Save image to MediaStore using scoped storage
            val savedUri = saveImageToScopedStorage(context, newUri, accessPoint.name, accessPoint.pictures.size + 1)
            savedUri?.let {
                // Update the list state by adding the new image URI string.
                fullSizeImagesState.value = fullSizeImagesState.value + it.toString()
                Log.d("APDetailScreen", "Added Image URI: $newUri")
                viewModel.updateAccessPoint(accessPoint.copy(pictures = fullSizeImagesState.value))
            }
        } else {
            Log.e("APDetailScreen", "Image capture failed or URI is null")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${accessPoint.name} Pictures") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectionMode && selectedPictures.isNotEmpty()) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val fileName = "${accessPoint.name}-${fullSizeImagesState.value.size + 1}.jpg"
                val file = File(context.filesDir, fileName)

                // Create a URI using FileProvider for the new file.
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.wavehitech.aptracker.provider",  // Must match the authority in your manifest
                    file
                )
                imageUri = uri // Persist the URI across rotation via our custom saver

                cameraLauncher.launch(uri)
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Picture")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (fullSizeImagesState.value.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No pictures available. Tap + to add.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(fullSizeImagesState.value) { fullImageUriString ->
                        val uri = Uri.parse(fullImageUriString)

                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .combinedClickable(
                                    onClick = {
                                        if (selectionMode) {
                                            if (selectedPictures.contains(fullImageUriString)) {
                                                selectedPictures.remove(fullImageUriString)
                                                if (selectedPictures.isEmpty()) selectionMode = false
                                            } else {
                                                selectedPictures.add(fullImageUriString)
                                            }
                                        } else {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "image/*")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                    onLongClick = {
                                        if (!selectionMode) {
                                            selectionMode = true
                                        }
                                        if (!selectedPictures.contains(fullImageUriString)) {
                                            selectedPictures.add(fullImageUriString)
                                        }
                                    }
                                )
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Full-size Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (selectionMode && selectedPictures.contains(fullImageUriString)) {
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

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion") },
            text = {
                val count = if (selectionMode) selectedPictures.size else 1
                Text("Are you sure you want to delete $count image(s)?")
            },
            confirmButton = {
                Button(onClick = {
                    if (selectionMode) {
                        selectedPictures.forEach { picUriString ->
                            val uri = Uri.parse(picUriString)
                            val fileName = uri.lastPathSegment
                            val file = File(context.filesDir, fileName)
                            if (file.exists()) file.delete()
                        }
                        fullSizeImagesState.value = fullSizeImagesState.value - selectedPictures
                        viewModel.updateAccessPoint(accessPoint.copy(pictures = fullSizeImagesState.value))
                        selectedPictures.clear()
                        selectionMode = false
                    } else {
                        val uri = Uri.parse(fullSizeImagesState.value.first())
                        val fileName = uri.lastPathSegment
                        if (fileName != null) {
                            val file = File(context.filesDir, fileName)
                            if (file.exists()) file.delete()
                        }
                        fullSizeImagesState.value = fullSizeImagesState.value.drop(1)
                        viewModel.updateAccessPoint(accessPoint.copy(pictures = fullSizeImagesState.value))
                    }
                    showDeleteConfirmation = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) {
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
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close")
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
    val zipFile = File(context.cacheDir, "${projectWithAP.project.name}.zip")
    val totalFiles = projectWithAP.accessPoints.sumOf { it.pictures.size }
    var processedFiles = 0

    ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
        for (ap in projectWithAP.accessPoints) {
            for ((index, pictureUriString) in ap.pictures.withIndex()) {
                val uri = Uri.parse(pictureUriString)
                val file = getFileFromUri(context, uri) ?: continue

                if (file.exists()) {
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

    CoroutineScope(Dispatchers.Main).launch {
        isZipping.value = false
        onDismiss() // ✅ Close the export dialog after zipping completes
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
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface
                )

                if (isZipping.value) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress.value / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!isZipping.value && warningMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = warningMessage,
                            color = Color.Red,
                            style = MaterialTheme.typography.body2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isZipping.value) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isZipping.value = true
                                CoroutineScope(Dispatchers.IO).launch {
                                    onShare(progress, isZipping, onDismiss)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Share") }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            isZipping.value = true
                            CoroutineScope(Dispatchers.IO).launch {
                                onSave(progress, isZipping, onDismiss)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save to Documents Folder") }
                }
            }
        }
    }
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
                label = { Text("AP Name (e.g., AP05)") }
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
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
