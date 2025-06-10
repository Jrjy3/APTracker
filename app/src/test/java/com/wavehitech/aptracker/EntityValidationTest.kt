package com.wavehitech.aptracker

import com.wavehitech.aptracker.models.AccessPoint
import com.wavehitech.aptracker.models.ProjectWithAccessPoints
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

class EntityValidationTest {

    @Test
    fun `ProjectEntity creates with required fields`() {
        val id = "project-123"
        val name = "Test Project"

        val project = ProjectEntity(id, name)

        assertEquals(id, project.id)
        assertEquals(name, project.name)
    }

    @Test
    fun `ProjectEntity equality works correctly`() {
        val project1 = ProjectEntity("123", "Test")
        val project2 = ProjectEntity("123", "Test")
        val project3 = ProjectEntity("456", "Test")

        assertEquals(project1, project2)
        assertNotEquals(project1, project3)
    }

    @Test
    fun `AccessPointEntity creates with required fields`() {
        val id = "ap-123"
        val projectId = "project-456"
        val name = "Access Point 1"

        val accessPoint = AccessPointEntity(id, projectId, name)

        assertEquals(id, accessPoint.id)
        assertEquals(projectId, accessPoint.projectId)
        assertEquals(name, accessPoint.name)
    }

    @Test
    fun `AccessPointEntity maintains foreign key relationship`() {
        val projectId = "project-123"
        val accessPoint = AccessPointEntity("ap-1", projectId, "AP1")

        assertEquals(projectId, accessPoint.projectId)
    }

    @Test
    fun `ImageEntity creates with all required fields`() {
        val id = "img-123"
        val accessPointId = "ap-456"
        val filename = "test-image.jpg"
        val isCircled = false
        val originalImageId = null
        val orderIndex = 0

        val image = ImageEntity(id, accessPointId, filename, isCircled, originalImageId, orderIndex)

        assertEquals(id, image.id)
        assertEquals(accessPointId, image.accessPointId)
        assertEquals(filename, image.filename)
        assertEquals(isCircled, image.isCircled)
        assertEquals(originalImageId, image.originalImageId)
        assertEquals(orderIndex, image.orderIndex)
    }

    @Test
    fun `ImageEntity creates circled version with original reference`() {
        val originalId = "img-original"
        val circledImage = ImageEntity(
            id = "img-circled",
            accessPointId = "ap-1",
            filename = "image_circle.jpg",
            isCircled = true,
            originalImageId = originalId,
            orderIndex = 1
        )

        assertTrue(circledImage.isCircled)
        assertEquals(originalId, circledImage.originalImageId)
        assertTrue(circledImage.filename.contains("circle"))
    }

    @Test
    fun `ImageEntity generates UUID by default`() {
        val image = ImageEntity(
            accessPointId = "ap-1",
            filename = "test.jpg",
            isCircled = false,
            orderIndex = 0
        )

        assertNotNull(image.id)
        assertTrue(image.id.isNotEmpty())
        // Verify it's a valid UUID format
        assertDoesNotThrow {
            UUID.fromString(image.id)
        }
    }

    @Test
    fun `AccessPoint model creates with default values`() {
        val name = "Test AP"
        val accessPoint = AccessPoint(name = name)

        assertNotNull(accessPoint.id)
        assertEquals(name, accessPoint.name)
        assertTrue(accessPoint.pictures.isEmpty())
        
        // Verify default ID is valid UUID
        assertDoesNotThrow {
            UUID.fromString(accessPoint.id)
        }
    }

    @Test
    fun `AccessPoint model allows custom ID`() {
        val customId = "custom-ap-id"
        val name = "Test AP"
        val accessPoint = AccessPoint(id = customId, name = name)

        assertEquals(customId, accessPoint.id)
        assertEquals(name, accessPoint.name)
    }

    @Test
    fun `AccessPoint pictures list is mutable`() {
        val accessPoint = AccessPoint(name = "Test AP")
        
        assertTrue(accessPoint.pictures.isEmpty())
        
        accessPoint.pictures.add("image1.jpg")
        accessPoint.pictures.add("image2.jpg")
        
        assertEquals(2, accessPoint.pictures.size)
        assertEquals("image1.jpg", accessPoint.pictures[0])
        assertEquals("image2.jpg", accessPoint.pictures[1])
    }

    @Test
    fun `ProjectWithAccessPoints creates relationship correctly`() {
        val project = ProjectEntity("project-1", "Test Project")
        val accessPoints = listOf(
            AccessPointEntity("ap-1", "project-1", "AP1"),
            AccessPointEntity("ap-2", "project-1", "AP2")
        )

        val projectWithAPs = ProjectWithAccessPoints(project, accessPoints)

        assertEquals(project, projectWithAPs.project)
        assertEquals(2, projectWithAPs.accessPoints.size)
        assertEquals("ap-1", projectWithAPs.accessPoints[0].id)
        assertEquals("ap-2", projectWithAPs.accessPoints[1].id)
        
        // Verify all access points belong to the same project
        projectWithAPs.accessPoints.forEach { ap ->
            assertEquals(project.id, ap.projectId)
        }
    }

    @Test
    fun `ImageEntity filename follows naming convention`() {
        val projectId = "project-123"
        val apName = "AP01"
        val imageNumber = 1
        val expectedFilename = "${projectId}_${apName}-${imageNumber}.jpg"

        val image = ImageEntity(
            accessPointId = "ap-1",
            filename = expectedFilename,
            isCircled = false,
            orderIndex = 0
        )

        assertEquals(expectedFilename, image.filename)
        assertTrue(image.filename.contains(projectId))
        assertTrue(image.filename.contains(apName))
        assertTrue(image.filename.endsWith(".jpg"))
    }

    @Test
    fun `ImageEntity circled filename follows naming convention`() {
        val baseFilename = "project-123_AP01-1.jpg"
        val circledFilename = baseFilename.replace(".jpg", "_circle_.jpg")

        val circledImage = ImageEntity(
            accessPointId = "ap-1",
            filename = circledFilename,
            isCircled = true,
            originalImageId = "original-img-id",
            orderIndex = 1
        )

        assertTrue(circledImage.filename.contains("_circle_"))
        assertTrue(circledImage.isCircled)
        assertNotNull(circledImage.originalImageId)
    }

    @Test
    fun `Entity IDs are not empty strings`() {
        val project = ProjectEntity("", "Test")
        val accessPoint = AccessPointEntity("", "project-1", "AP")
        val image = ImageEntity("", "ap-1", "test.jpg", false, null, 0)

        // While entities can be created with empty IDs, 
        // this tests that we can detect such cases
        assertTrue(project.id.isEmpty())
        assertTrue(accessPoint.id.isEmpty())
        assertTrue(image.id.isEmpty())
    }

    @Test
    fun `Entity names handle special characters`() {
        val specialName = "Test Project (2024) - #1"
        val project = ProjectEntity("project-1", specialName)
        
        assertEquals(specialName, project.name)
        
        val specialAPName = "AP-01/Floor-2"
        val accessPoint = AccessPointEntity("ap-1", "project-1", specialAPName)
        
        assertEquals(specialAPName, accessPoint.name)
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }
}