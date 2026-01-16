package com.example.cometmusic.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import androidx.media3.session.MediaController;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cometmusic.ui.songlist.SongAdapter;
import com.example.cometmusic.utils.factory.GlideProvider;
import com.google.android.material.card.MaterialCardView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class SongAdapterTest {
    private SongAdapter songAdapter;

    @Mock
    private Context mockContext;

    @Mock
    private MediaController mockPlayer;

    @Mock
    private List<Song> mockSongs;

    @Mock
    private RecyclerView mockRecyclerView;

    @Mock
    private Handler mockHandler;

    @Mock
    private LinearLayoutManager mockLayoutManager;

    @Mock
    private GlideProvider mockGlideProvider;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockContext = mock(Context.class);
        mockPlayer = mock(MediaController.class);
        mockSongs = new ArrayList<>();
        mockHandler = spy(new Handler());
        int playlistPosition = 0;
        long id = 456;
        String name = "test";
        Uri uri = Uri.parse("https://test");
        Uri albumArtworkUri = Uri.parse("https://albumtest");
        int size = 123456;
        long duration = 123000L;
        Song song = new Song(playlistPosition, id, name, uri, albumArtworkUri, size, duration);
        mockSongs.add(song);
        mockRecyclerView = mock(RecyclerView.class);

        mockLayoutManager = mock(LinearLayoutManager.class);
        doReturn(mockLayoutManager).when(mockRecyclerView).getLayoutManager();

        mockGlideProvider = mock(GlideProvider.class);

        songAdapter = spy(new SongAdapter(mockContext, mockPlayer, mockSongs, mockRecyclerView, mockGlideProvider));
    }

    @Test
    public void songAdapter_StartCountdownTimer_DelayedToShowVisibleBitmaps() {
        doNothing().when(songAdapter).showVisibleBitmaps();
        songAdapter.setCountTimeHandler(mockHandler);
        ShadowLooper shadowLooper = shadowOf(mockHandler.getLooper());

        // call the method that should be tested
        songAdapter.startCountdownTimer();

        assertTrue(songAdapter.getScrolling());

        verify(mockHandler).removeCallbacksAndMessages(null);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockHandler).postDelayed(runnableCaptor.capture(), eq((long) SongAdapter.LOAD_COVER_IMAGE_AFTER_STOP_SCROLLING_MILLISECOND));

        shadowLooper.runOneTask();
        assertFalse(songAdapter.getScrolling());
        verify(songAdapter).showVisibleBitmaps();
    }

    @Test
    public void songAdapter_ShowVisibleBitmaps_NotifyFromStartToEndItemChanged() {

        int start = 123, end = 456;
        doReturn(start).when(mockLayoutManager).findFirstVisibleItemPosition();
        doReturn(end).when(mockLayoutManager).findLastVisibleItemPosition();

        // call the method that should be tested;
        songAdapter.showVisibleBitmaps();

        verify(songAdapter).notifyItemsChangedOneByOne(start, end);
    }

    @Test
    public void notifyItemsChangedOneByOne_ShouldNotifyItemsInRange() {
        Handler mockBackgroundHandler = mock(Handler.class);
        songAdapter.setBackgroundHandler(mockBackgroundHandler);

        // https://stackoverflow.com/a/44235999/19850087
        when(mockBackgroundHandler.postDelayed(any(Runnable.class), eq(SongAdapter.DELAY_BETWEEN_ITEMS_MILLISECOND)))
                .thenAnswer((Answer<Void>) invocation -> {
                    invocation.<Runnable>getArgument(0).run();
                    return null;
                });

        int start = 0;
        int end = 5;

        // call the method that should be tested
        songAdapter.notifyItemsChangedOneByOne(start, end);

        // verify that notifyItemChanged is called for each item in the range
        for (int i = start; i <= end; i++) {
            verify(songAdapter).notifyItemChanged(i);
        }
    }

    @Test
    public void songAdapter_ClearImageCache_CallGlideClearMemory() {
        Glide mockGlide = mock(Glide.class);

        doReturn(mockGlide).when(mockGlideProvider).getGlideInstance(mockContext);

        // call the method that should be tested
        songAdapter.clearImageCache();

        verify(mockGlide).clearMemory();
    }

    @Test
    public void songAdapter_SetPlayerControllerListener_ShouldCorrectlySet() {
        SongAdapter.PlayerControlListener listener = mock(SongAdapter.PlayerControlListener.class);

        // call the method that should be tested
        songAdapter.setPlayerControlListener(listener);

        assertEquals(listener, songAdapter.getPlayerControllerListener());
    }

    @Test
    public void OnCardViewClicked_WhenPlayerIsNull_ReturnsEarly() {
        songAdapter = spy(new SongAdapter(mockContext, null, mockSongs, mockRecyclerView, mockGlideProvider));

        MaterialCardView mockCardView = mock(MaterialCardView.class);

        // call the method that should be tested
        songAdapter.onCardViewClicked(1, null, mockCardView);

        verify(mockCardView, never()).setOnClickListener(any());
    }

    @Test
    public void OnCardViewClicked_WhenPlayerIsNotNull_SetClickListener() {
        MaterialCardView mockCardView = mock(MaterialCardView.class);

        // call the method that should be tested
        songAdapter.onCardViewClicked(1, null, mockCardView);

        verify(mockCardView).setOnClickListener(any());
    }

    @Test
    public void ClearViewBorder_WhenPreviousCardHolderIsNull_NotExecuteSetStrokeWidth() {
        songAdapter.setPreviousCardHolder(null);


        try {
            // call the method that should be tested
            songAdapter.clearViewBorder(false);
        }
        catch (Exception e) {
            Assert.fail("clearViewBorder should not throw any exception when previousCardHolder is null");
        }
    }

    @Test
    public void ClearViewBorder_WhenPreviousCardHolderIsNotNullStrokeWidthIsNotZero_NotExecuteSetStrokeWidth() {
        MaterialCardView mockPreviousCardHolder = mock(MaterialCardView.class);

        doReturn(0).when(mockPreviousCardHolder).getStrokeWidth();

        songAdapter.setPreviousCardHolder(mockPreviousCardHolder);

        // call the method that should be tested
        songAdapter.clearViewBorder(false);

        verify(mockPreviousCardHolder, never()).setStrokeWidth(anyInt());
    }

    @Test
    public void ClearViewBorder_WhenPreviousCardHolderIsNotNullAndStrokeWidthIsZero_SetStrokeWidth() {
        MaterialCardView mockPreviousCardHolder = mock(MaterialCardView.class);

        songAdapter.setPreviousCardHolder(mockPreviousCardHolder);

        doReturn(SongAdapter.STROKE_WIDTH).when(mockPreviousCardHolder).getStrokeWidth();

        // call the method that should be tested
        songAdapter.clearViewBorder(false);

        verify(mockPreviousCardHolder).setStrokeWidth(0);
    }

    @Test
    public void ClearViewBorder_WhenPreviousViewHolderIsNull_NotExecuteSetStrokeWidth() {
        int mockPreviousIndex = 5;
        songAdapter.setPreviousIndex(mockPreviousIndex);
        doReturn(null).when(mockRecyclerView).findViewHolderForAdapterPosition(mockPreviousIndex);

        try {
            // call the method that should be tested
            songAdapter.clearViewBorder(false);
        }
        catch (Exception e) {
            Assert.fail("clearViewBorder should not throw any exception when previousViewHolder is null");
        }
    }
}
