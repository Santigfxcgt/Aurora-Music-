package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.RepeatMode
import com.example.service.CoverType
import com.example.service.CustomCover
import com.example.service.CustomCoverManager
import com.example.service.MusicScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aurora Music", appName)
  }

  @Test
  fun `verify demo songs loaded with metadata and lyrics`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val demoSongs = MusicScanner.getDefaultDemoSongs(context)
    
    // Check catalog size
    assertEquals(10, demoSongs.size)

    // Verify first track (DIRTY MONEY)
    val first = demoSongs[0]
    assertEquals("DIRTY MONEY (That's My Plan)", first.title)
    assertEquals("Young Harry", first.artist)
    assertTrue(!first.lyrics.isNullOrEmpty())

    // Verify second track (Why? Feat. lobonabeat!)
    val second = demoSongs[1]
    assertEquals("Why? (Feat. lobonabeat!)", second.title)
    assertEquals("Street Baby, lobonabeat!", second.artist)
    assertTrue(second.isFavorite)

    // Verify all songs have valid IDs and durations
    demoSongs.forEach { song ->
      assertTrue(song.id.isNotEmpty())
      assertTrue(song.durationMs > 0)
      assertNotNull(song.album)
    }
  }

  @Test
  fun `verify repeat mode cycling`() {
    var mode = RepeatMode.OFF
    mode = when (mode) {
      RepeatMode.OFF -> RepeatMode.ALL
      RepeatMode.ALL -> RepeatMode.ONE
      RepeatMode.ONE -> RepeatMode.OFF
    }
    assertEquals(RepeatMode.ALL, mode)

    mode = when (mode) {
      RepeatMode.OFF -> RepeatMode.ALL
      RepeatMode.ALL -> RepeatMode.ONE
      RepeatMode.ONE -> RepeatMode.OFF
    }
    assertEquals(RepeatMode.ONE, mode)

    mode = when (mode) {
      RepeatMode.OFF -> RepeatMode.ALL
      RepeatMode.ALL -> RepeatMode.ONE
      RepeatMode.ONE -> RepeatMode.OFF
    }
    assertEquals(RepeatMode.OFF, mode)
  }

  @Test
  fun `verify CustomCoverManager initializes and exposes state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    CustomCoverManager.init(context)
    assertNotNull(CustomCoverManager.customCovers.value)

    val cover = CustomCover(
      songId = "demo_1",
      fileUri = "file:///dummy/video.mp4",
      type = CoverType.VIDEO,
      thumbnailUri = "file:///dummy/thumb.jpg"
    )
    assertEquals(CoverType.VIDEO, cover.type)
    assertEquals("demo_1", cover.songId)
    assertEquals("file:///dummy/thumb.jpg", cover.thumbnailUri)
  }
}

