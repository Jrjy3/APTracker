package com.wavehitech.aptracker

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    @Test
    fun `fromStringList converts list to string with separator`() {
        val list = listOf("item1", "item2", "item3")
        
        val result = converters.fromStringList(list)
        
        assertEquals("item1;;item2;;item3", result)
    }

    @Test
    fun `fromStringList handles empty list`() {
        val list = emptyList<String>()
        
        val result = converters.fromStringList(list)
        
        assertEquals("", result)
    }

    @Test
    fun `fromStringList handles single item`() {
        val list = listOf("singleItem")
        
        val result = converters.fromStringList(list)
        
        assertEquals("singleItem", result)
    }

    @Test
    fun `fromStringList handles items with special characters`() {
        val list = listOf("item with spaces", "item/with/slashes", "item-with-dashes")
        
        val result = converters.fromStringList(list)
        
        assertEquals("item with spaces;;item/with/slashes;;item-with-dashes", result)
    }

    @Test
    fun `fromStringList handles items containing separator`() {
        // This tests edge case where items contain the separator string
        val list = listOf("item;;with;;separators", "normal item")
        
        val result = converters.fromStringList(list)
        
        assertEquals("item;;with;;separators;;normal item", result)
    }

    @Test
    fun `toStringList converts string to list with separator`() {
        val data = "item1;;item2;;item3"
        
        val result = converters.toStringList(data)
        
        assertEquals(listOf("item1", "item2", "item3"), result)
    }

    @Test
    fun `toStringList handles empty string`() {
        val data = ""
        
        val result = converters.toStringList(data)
        
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `toStringList handles single item`() {
        val data = "singleItem"
        
        val result = converters.toStringList(data)
        
        assertEquals(listOf("singleItem"), result)
    }

    @Test
    fun `toStringList handles items with special characters`() {
        val data = "item with spaces;;item/with/slashes;;item-with-dashes"
        
        val result = converters.toStringList(data)
        
        assertEquals(listOf("item with spaces", "item/with/slashes", "item-with-dashes"), result)
    }

    @Test
    fun `toStringList handles string with trailing separator`() {
        val data = "item1;;item2;;"
        
        val result = converters.toStringList(data)
        
        assertEquals(listOf("item1", "item2", ""), result)
    }

    @Test
    fun `toStringList handles string with leading separator`() {
        val data = ";;item1;;item2"
        
        val result = converters.toStringList(data)
        
        assertEquals(listOf("", "item1", "item2"), result)
    }

    @Test
    fun `toStringList handles consecutive separators`() {
        val data = "item1;;;;item2"
        
        val result = converters.toStringList(data)
        
        assertEquals(listOf("item1", "", "item2"), result)
    }

    @Test
    fun `toStringList handles only separators`() {
        val data = ";;;;"
        
        val result = converters.toStringList(data)
        
        assertEquals(listOf("", "", ""), result)
    }

    @Test
    fun `roundtrip conversion preserves original list`() {
        val originalList = listOf("item1", "item2", "item3", "item with spaces")
        
        val stringData = converters.fromStringList(originalList)
        val convertedBackList = converters.toStringList(stringData)
        
        assertEquals(originalList, convertedBackList)
    }

    @Test
    fun `roundtrip conversion preserves empty list`() {
        val originalList = emptyList<String>()
        
        val stringData = converters.fromStringList(originalList)
        val convertedBackList = converters.toStringList(stringData)
        
        assertEquals(originalList, convertedBackList)
    }

    @Test
    fun `roundtrip conversion preserves single item list`() {
        val originalList = listOf("single item")
        
        val stringData = converters.fromStringList(originalList)
        val convertedBackList = converters.toStringList(stringData)
        
        assertEquals(originalList, convertedBackList)
    }

    @Test
    fun `roundtrip conversion with complex items`() {
        val originalList = listOf(
            "file_name.jpg",
            "path/to/file.png",
            "item with spaces and numbers 123",
            "",  // empty string item
            "item-with-dashes_and_underscores"
        )
        
        val stringData = converters.fromStringList(originalList)
        val convertedBackList = converters.toStringList(stringData)
        
        assertEquals(originalList, convertedBackList)
    }

    @Test
    fun `converter handles unicode characters`() {
        val originalList = listOf("测试", "тест", "🎯", "café")
        
        val stringData = converters.fromStringList(originalList)
        val convertedBackList = converters.toStringList(stringData)
        
        assertEquals(originalList, convertedBackList)
    }

    @Test
    fun `separator is consistently used`() {
        val separator = ";;"
        val list = listOf("a", "b", "c")
        
        val result = converters.fromStringList(list)
        
        assertTrue(result.contains(separator))
        assertEquals(2, result.split(separator).size - 1) // Should have 2 separators for 3 items
    }
}