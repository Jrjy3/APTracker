package com.wavehitech.aptracker

import android.content.Context
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ProjectRepositoryTest {

    @Mock
    private lateinit var projectDao: ProjectDao

    @Mock
    private lateinit var accessPointDao: AccessPointDao

    @Mock
    private lateinit var imageDao: ImageDao

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var mockFile: File

    private lateinit var repository: ProjectRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock context.filesDir
        `when`(context.filesDir).thenReturn(mockFile)
        
        repository = ProjectRepository(projectDao, accessPointDao, imageDao, context)
    }

    @Test
    fun `getAllProjectsFlow returns flow from dao`() = runTest {
        val projects = listOf(
            ProjectEntity("1", "Project 1", "Description 1"),
            ProjectEntity("2", "Project 2", "Description 2")
        )
        `when`(projectDao.getAllProjectsFlow()).thenReturn(flowOf(projects))

        val result = repository.getAllProjectsFlow().first()

        assertEquals(projects, result)
        verify(projectDao).getAllProjectsFlow()
    }

    @Test
    fun `addProjectWithAccessPoints inserts project and access points`() = runTest {
        val project = ProjectEntity("1", "Test Project", "Description")
        val accessPoints = listOf(
            AccessPointEntity("ap1", "1", "AP 1", "Location 1"),
            AccessPointEntity("ap2", "1", "AP 2", "Location 2")
        )

        repository.addProjectWithAccessPoints(project, accessPoints)

        verify(projectDao).insertProject(project)
        verify(accessPointDao).insertAccessPoint(accessPoints[0])
        verify(accessPointDao).insertAccessPoint(accessPoints[1])
    }

    @Test
    fun `getAccessPointsForProjectFlow returns flow from dao`() = runTest {
        val projectId = "project1"
        val accessPoints = listOf(
            AccessPointEntity("ap1", projectId, "AP 1", "Location 1")
        )
        `when`(accessPointDao.getAccessPointsForProjectFlow(projectId)).thenReturn(flowOf(accessPoints))

        val result = repository.getAccessPointsForProjectFlow(projectId).first()

        assertEquals(accessPoints, result)
        verify(accessPointDao).getAccessPointsForProjectFlow(projectId)
    }

    @Test
    fun `updateAccessPoint calls dao update`() = runTest {
        val accessPoint = AccessPointEntity("ap1", "project1", "Updated AP", "New Location")

        repository.updateAccessPoint(accessPoint)

        verify(accessPointDao).updateAccessPoint(accessPoint)
    }

    @Test
    fun `addAccessPoint calls dao insert`() = runTest {
        val accessPoint = AccessPointEntity("ap1", "project1", "New AP", "Location")

        repository.addAccessPoint(accessPoint)

        verify(accessPointDao).insertAccessPoint(accessPoint)
    }

    @Test
    fun `getAccessPointsForProject returns list from dao`() = runTest {
        val projectId = "project1"
        val accessPoints = listOf(
            AccessPointEntity("ap1", projectId, "AP 1", "Location 1")
        )
        `when`(accessPointDao.getAccessPointsForProject(projectId)).thenReturn(accessPoints)

        val result = repository.getAccessPointsForProject(projectId)

        assertEquals(accessPoints, result)
        verify(accessPointDao).getAccessPointsForProject(projectId)
    }

    @Test
    fun `getAccessPointsForProject returns empty list on exception`() = runTest {
        val projectId = "project1"
        `when`(accessPointDao.getAccessPointsForProject(projectId)).thenThrow(RuntimeException("DB Error"))

        val result = repository.getAccessPointsForProject(projectId)

        assertEquals(emptyList<AccessPointEntity>(), result)
    }

    @Test
    fun `deleteAccessPoint deletes images and files`() = runTest {
        val accessPointId = "ap1"
        val images = listOf(
            ImageEntity("img1", accessPointId, "image1.jpg", 0, null),
            ImageEntity("img2", accessPointId, "image2.jpg", 1, null)
        )
        
        `when`(imageDao.getImagesForAccessPointFlow(accessPointId)).thenReturn(flowOf(images))
        `when`(imageDao.deleteAllImagesForAccessPoint(accessPointId)).thenReturn(2)
        `when`(imageDao.checkForImagesWithAccessPoint(accessPointId)).thenReturn(emptyList())

        // Mock file operations
        val mockFile1 = mock(File::class.java)
        val mockFile2 = mock(File::class.java)
        `when`(mockFile1.exists()).thenReturn(true)
        `when`(mockFile1.delete()).thenReturn(true)
        `when`(mockFile2.exists()).thenReturn(false)

        mockStatic(File::class.java).use { mockedFile ->
            mockedFile.`when`<File> { File(mockFile, "image1.jpg") }.thenReturn(mockFile1)
            mockedFile.`when`<File> { File(mockFile, "image2.jpg") }.thenReturn(mockFile2)

            repository.deleteAccessPoint(accessPointId)

            verify(mockFile1).delete()
            verify(mockFile2, never()).delete()
            verify(imageDao).deleteAllImagesForAccessPoint(accessPointId)
            verify(accessPointDao).deleteAccessPoint(accessPointId)
        }
    }

    @Test
    fun `deleteProjectWithAccessPoints deletes all associated files`() = runTest {
        val projectId = "project1"
        val accessPoints = listOf(
            AccessPointEntity("ap1", projectId, "AP 1", "Location 1")
        )
        val images = listOf(
            ImageEntity("img1", "ap1", "image1.jpg", 0, null)
        )

        `when`(accessPointDao.getAccessPointsForProjectFlow(projectId)).thenReturn(flowOf(accessPoints))
        `when`(imageDao.getImagesForAccessPointFlow("ap1")).thenReturn(flowOf(images))

        val mockImageFile = mock(File::class.java)
        `when`(mockImageFile.exists()).thenReturn(true)
        `when`(mockImageFile.delete()).thenReturn(true)

        mockStatic(File::class.java).use { mockedFile ->
            mockedFile.`when`<File> { File(mockFile, "image1.jpg") }.thenReturn(mockImageFile)

            repository.deleteProjectWithAccessPoints(projectId)

            verify(mockImageFile).delete()
            verify(projectDao).deleteProject(projectId)
        }
    }

    @Test
    fun `getAccessPointById returns access point from dao`() = runTest {
        val apId = "ap1"
        val accessPoint = AccessPointEntity(apId, "project1", "AP 1", "Location 1")
        `when`(accessPointDao.getAccessPointById(apId)).thenReturn(accessPoint)

        val result = repository.getAccessPointById(apId)

        assertEquals(accessPoint, result)
        verify(accessPointDao).getAccessPointById(apId)
    }

    @Test
    fun `getAccessPointById returns null on exception`() = runTest {
        val apId = "ap1"
        `when`(accessPointDao.getAccessPointById(apId)).thenThrow(RuntimeException("DB Error"))

        val result = repository.getAccessPointById(apId)

        assertNull(result)
    }

    @Test
    fun `getImagesForAccessPointFlow returns flow from dao`() = runTest {
        val apId = "ap1"
        val images = listOf(
            ImageEntity("img1", apId, "image1.jpg", 0, null)
        )
        `when`(imageDao.getImagesForAccessPointFlow(apId)).thenReturn(flowOf(images))

        val result = repository.getImagesForAccessPointFlow(apId).first()

        assertEquals(images, result)
        verify(imageDao).getImagesForAccessPointFlow(apId)
    }

    @Test
    fun `insertImage calls dao insert`() = runTest {
        val image = ImageEntity("img1", "ap1", "image1.jpg", 0, null)

        repository.insertImage(image)

        verify(imageDao).insertImage(image)
    }

    @Test
    fun `updateImage calls dao update`() = runTest {
        val image = ImageEntity("img1", "ap1", "image1.jpg", 0, null)

        repository.updateImage(image)

        verify(imageDao).updateImage(image)
    }

    @Test
    fun `deleteImage calls dao delete`() = runTest {
        val imageId = "img1"

        repository.deleteImage(imageId)

        verify(imageDao).deleteImage(imageId)
    }

    @Test
    fun `getImageById returns image from dao`() = runTest {
        val imageId = "img1"
        val image = ImageEntity(imageId, "ap1", "image1.jpg", 0, null)
        `when`(imageDao.getImageById(imageId)).thenReturn(image)

        val result = repository.getImageById(imageId)

        assertEquals(image, result)
        verify(imageDao).getImageById(imageId)
    }

    @Test
    fun `getCircledVersionOfImage returns circled image from dao`() = runTest {
        val originalId = "img1"
        val circledImage = ImageEntity("img2", "ap1", "image1_circle.jpg", 1, originalId)
        `when`(imageDao.getCircledVersionOfImage(originalId)).thenReturn(circledImage)

        val result = repository.getCircledVersionOfImage(originalId)

        assertEquals(circledImage, result)
        verify(imageDao).getCircledVersionOfImage(originalId)
    }

    @Test
    fun `updateImageOrder calls dao update`() = runTest {
        val imageId = "img1"
        val newIndex = 5

        repository.updateImageOrder(imageId, newIndex)

        verify(imageDao).updateImageOrder(imageId, newIndex)
    }
}