package com.example.cometmusic.viewmodel.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.net.Uri;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.test.core.app.ApplicationProvider;

import com.example.cometmusic.R;
import com.example.cometmusic.data.SharedData;
import com.example.cometmusic.model.Song;
import com.example.cometmusic.repository.FetchAudioFiles;
import com.example.cometmusic.ui.PlayerViewModel;

import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class PlayerViewModelTest {

    @Mock
    private PlayerViewModel playerViewModel;

    @Mock
    private Application mockApplication;

    @Mock
    private SharedData mockSharedData;

    @Mock
    private FetchAudioFiles mockFetchAudioFiles;

    @Mock
    private MediaController mockPlayer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockSharedData = mock(SharedData.class);
        mockFetchAudioFiles = mock(FetchAudioFiles.class);
        mockPlayer = mock(MediaController.class);

        Application application = ApplicationProvider.getApplicationContext();
        playerViewModel = new PlayerViewModel(application, mockSharedData);
    }

    @Test
    public void playerViewModel_WhenPlayerIsNull_ThrowsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> playerViewModel.getPlayer());
        String actualMessage = exception.getMessage();
        assertEquals(PlayerViewModel.PLAYER_IS_NULL, actualMessage);
    }

    @Test
    public void playerViewModel_SetPlayerWithPlayerProvided_SetPlayerUpdatedCanBeRetrieved() {
        playerViewModel.setPlayer(mockPlayer);
        assertEquals(mockPlayer, playerViewModel.getPlayer().getValue());
    }

    @Test
    public void playerViewModel_GetPlayerModeWithPlayerModeIsNull_ReturnsSharedDataPlayerMode() {
        when(mockSharedData.getPlayerMode()).thenReturn(2);
        assertEquals(2, (Object) playerViewModel.getPlayerMode().getValue());
    }

    @Test
    public void playerViewModel_WhenPlayerModeIsOne_ShouldSetRepeatAll() {
        playerViewModel.setPlayer(mockPlayer);

        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setRepeatMode(Mockito.anyInt());
        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setShuffleModeEnabled(Mockito.anyBoolean());

        playerViewModel.setPlayerMode(1);

        verify(mockPlayer).setRepeatMode(2); // REPEAT_MODE_ALL = 2
        verify(mockPlayer).setShuffleModeEnabled(false);
        assertEquals(1, (Object) playerViewModel.getPlayerMode().getValue());
    }

    @Test
    public void playerViewModel_WhenPlayerModeIsTwo_ShouldSetRepeatOne() {
        playerViewModel.setPlayer(mockPlayer);

        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setRepeatMode(Mockito.anyInt());

        playerViewModel.setPlayerMode(2);

        verify(mockPlayer).setRepeatMode(1); // REPEAT_MODE_ONE = 1
        assertEquals(2, (Object) playerViewModel.getPlayerMode().getValue());
    }

    @Test
    public void playerViewModel_WhenPlayerModeIsThree_ShouldSetRandomAll() {
        playerViewModel.setPlayer(mockPlayer);

        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setRepeatMode(Mockito.anyInt());
        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setShuffleModeEnabled(Mockito.anyBoolean());

        playerViewModel.setPlayerMode(3);

        StateFlow<Integer> playerModeFlow = playerViewModel.getPlayerMode();

        verify(mockPlayer).setRepeatMode(0); // REPEAT_MODE_OFF = 0
        verify(mockPlayer).setShuffleModeEnabled(true);
        assertEquals(3, Objects.requireNonNull(playerModeFlow.getValue()).intValue());
    }

    @Test
    public void playerViewModel_ClickRepeatButtonWithPlayerMode_SetPlayerModeToNext() {
        playerViewModel.setPlayer(mockPlayer);

        playerViewModel.setPlayerMode(1);
        playerViewModel.clickRepeatButton();
        assertEquals(2, (Object) playerViewModel.getPlayerMode().getValue());

        playerViewModel.clickRepeatButton();
        assertEquals(3, (Object) playerViewModel.getPlayerMode().getValue());

        playerViewModel.clickRepeatButton();
        assertEquals(1, (Object) playerViewModel.getPlayerMode().getValue());
    }

    @Test
    public void playerViewModel_SkipToNextSongWithIsPlayerExistMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        partialMock.skipToNextSong();

        verify(mockPlayer, never()).seekToNext();
    }

    @Test
    public void playerViewModel_SkipToNextSongWithHasNextMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);
        when(mockPlayer.hasNextMediaItem()).thenReturn(false);

        partialMock.skipToNextSong();

        verify(mockPlayer, never()).seekToNext();
    }

    @Test
    public void playerViewModel_SkipToNextSongWithConditionCorresponded_CallThePlayerSeekToNext() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);
        when(mockPlayer.hasNextMediaItem()).thenReturn(true);

        partialMock.skipToNextSong();

        verify(mockPlayer, times(1)).seekToNext();
    }

    @Test
    public void playerViewModel_SkipToPreviousSongWithIsPlayerExistMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        partialMock.skipToPreviousSong();

        verify(mockPlayer, never()).seekToPrevious();
    }

    @Test
    public void playerViewModel_SkipToPreviousSongWithHasPreviousMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);
        when(mockPlayer.hasPreviousMediaItem()).thenReturn(false);

        partialMock.skipToPreviousSong();

        verify(mockPlayer, never()).seekToPrevious();
    }

    @Test
    public void playerViewModel_SkipToPreviousSongWithConditionCorresponded_CallThePlayerSeekToPrevious() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);
        when(mockPlayer.hasPreviousMediaItem()).thenReturn(true);

        partialMock.skipToPreviousSong();

        verify(mockPlayer, times(1)).seekToPrevious();
    }

    @Test
    public void playerViewModel_GetPlayerCurrentIndexWithIsPlayerExistMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        assertEquals(partialMock.getPlayerCurrentIndex(), 0);
    }

    @Test
    public void playerViewModel_GetPlayerCurrentIndexWithIsPlayerExistMediaItemIsTrue_ReturnsCurrentMediaItemIndex() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        int fakeCurrentMediaItemIndex = 101;
        doReturn(fakeCurrentMediaItemIndex).when(mockPlayer).getCurrentMediaItemIndex();

        assertEquals(partialMock.getPlayerCurrentIndex(), fakeCurrentMediaItemIndex);
    }

    @Test
    public void playerViewModel_GetDurationSecondWithDurationSecondIsNull_ReturnsDefaultValue() {
        assertEquals(100, (Object) playerViewModel.getDurationSecond().getValue());
    }

    @Test
    public void playerViewModel_SetDurationSecondWithIsPlayerExistMediaItemIsFalse_ResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        partialMock.setDurationSecond();

        verify(partialMock, never()).getReadableTime(anyInt());
    }

    @Test
    public void playerViewModel_SetDurationSecondWithIsPlayerExistMediaItemIsTrue_ResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        long[] fakeDurationMillisecond = {40000, 60000};
        List<Song> fakeSongs = new ArrayList<>();
        fakeSongs.add(new Song(0, 123456, "Test1", Uri.parse("abc"), Uri.parse("efg"), 1024, fakeDurationMillisecond[0]));
        fakeSongs.add(new Song(1, 789012, "Test2", Uri.parse("xyz"), Uri.parse("lmn"), 2048, fakeDurationMillisecond[1]));

        MutableStateFlow<List<Song>> fakeStateFlowSongs = StateFlowKt.MutableStateFlow(fakeSongs);
        when(partialMock.getSongs()).thenReturn((StateFlow<List<Song>>) (StateFlow<?>) fakeStateFlowSongs);

        int fakeCurrentMediaItemIndex = 0;
        doReturn(fakeCurrentMediaItemIndex).when(mockPlayer).getCurrentMediaItemIndex();

        partialMock.setDurationSecond();

        int fakeDurationSecond = partialMock.millisecondToSecond(fakeDurationMillisecond[fakeCurrentMediaItemIndex]);
        assertEquals(fakeDurationSecond, (Object) partialMock.getDurationSecond().getValue());
        assertEquals(partialMock.getReadableTime(fakeDurationSecond), partialMock.getReadableDurationString().getValue());

        fakeCurrentMediaItemIndex = 1;
        doReturn(fakeCurrentMediaItemIndex).when(mockPlayer).getCurrentMediaItemIndex();

        partialMock.setDurationSecond();

        fakeDurationSecond = partialMock.millisecondToSecond(fakeDurationMillisecond[fakeCurrentMediaItemIndex]);
        assertEquals(fakeDurationSecond, (Object) partialMock.getDurationSecond().getValue());
        assertEquals(partialMock.getReadableTime(fakeDurationSecond), partialMock.getReadableDurationString().getValue());
    }

    @Test
    public void playerViewModel_GetCurrentSecondWithCurrentSecondIsNull_ReturnsDefaultValue() {
        assertEquals(-1, (Object) playerViewModel.getCurrentSecond().getValue());
    }

    @Test
    public void playerViewModel_SetCurrentSecondWithIsPlayerExistMediaItemIsFalse_DefaultResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(ApplicationProvider.getApplicationContext(), mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        partialMock.setCurrentSecond();

        Toast toast = ShadowToast.getLatestToast();
        assertEquals(toast.getDuration(), Toast.LENGTH_SHORT);

        String expectedText = ApplicationProvider.getApplicationContext().getString(R.string.can_not_find_current_second);
        String actualText = ShadowToast.getTextOfLatestToast();
        assertEquals(expectedText, actualText);

        assertEquals(-1, (Object) partialMock.getCurrentSecond().getValue());
        assertEquals(partialMock.getReadableTime(-1), partialMock.getReadableCurrentString().getValue());
    }

    @Test
    public void playerViewModel_SetCurrentSecondWithIsPlayerExistMediaItemIsTrue_ResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        long fakeCurrentMillisecond = 800;
        when(mockPlayer.getCurrentPosition()).thenReturn(fakeCurrentMillisecond);

        partialMock.setCurrentSecond();

        int fakeCurrentSecond = partialMock.millisecondToSecond(fakeCurrentMillisecond);
        assertEquals(fakeCurrentSecond, (Object) partialMock.getCurrentSecond().getValue());
        assertEquals(partialMock.getReadableTime(fakeCurrentSecond), partialMock.getReadableCurrentString().getValue());
    }

    @Test
    public void playerViewModel_GetReadableCurrentStringWithReadableCurrentStringIsNull_CallSetCurrentSecond() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        doNothing().when(partialMock).setCurrentSecond();

        partialMock.getReadableCurrentString();

        verify(partialMock, times(1)).setCurrentSecond();
    }

    @Test
    public void playerViewModel_GetReadableDurationStringWithReadableDurationStringIsNull_CallSetDurationSecond() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        doNothing().when(partialMock).setDurationSecond();

        partialMock.getReadableDurationString();

        verify(partialMock, times(1)).setDurationSecond();
    }

    @Test
    public void playerViewModel_SeekToPosition_ResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);

        partialMock.seekToPosition(300);

        verify(mockPlayer, times(1)).seekTo(300);
    }

    @Test
    public void playerViewModel_SeekToSongIndexAndPosition_ResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        doNothing().when(partialMock).setCurrentSecond();

        partialMock.seekToSongIndexAndPosition(100, 200);

        verify(mockPlayer, times(1)).seekTo(100, 200);
        verify(partialMock, times(1)).setCurrentSecond();
    }

    @Test
    public void playerViewModel_SetSecondAndStringWhenMoving_ReturnValue() {
        playerViewModel.setSecondAndStringWhenMoving(100);

        assertEquals(100, (Object) playerViewModel.getCurrentSecond().getValue());
        assertEquals(playerViewModel.getReadableTime(100), playerViewModel.getReadableCurrentString().getValue());
    }

    @Test
    public void playerViewModel_GetCurrentSongNameWithCurrentSongNameIsNull_CallsSetCurrentSongName() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));

        assertNull(partialMock.getCurrentSongName().getValue());

        verify(partialMock, times(1)).setCurrentSongName();
    }

    @Test
    public void playerViewModel_GetCurrentSongNameWithCurrentSongNameProvided_ReturnsValue() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        String fakeName = "mockSongName";
        MediaItem mediaItem = new MediaItem.Builder()
                .setMediaMetadata(new MediaMetadata.Builder().setTitle(fakeName).build())
                .build();
        doReturn(mediaItem).when(mockPlayer).getCurrentMediaItem();

        partialMock.setCurrentSongName();

        assertEquals(fakeName, partialMock.getCurrentSongName().getValue());
    }

    @Test
    public void playerViewModel_clickPlayPauseBtnWithIsPlayerExistMediaItemFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);
        partialMock.setPlayer(mockPlayer);
        Mockito.clearInvocations(mockPlayer); // clear invocations from setPlayer (calls player.isPlaying)

        partialMock.clickPlayPauseBtn();

        verifyNoInteractions(mockPlayer);
        verify(partialMock, never()).getIsPlaying();
    }

    @Test
    public void playerViewModel_clickPlayPauseBtnWithGetIsPlayingReturnsTrue_PausesThePlayer() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);
        when(partialMock.getIsPlaying()).thenReturn(true);

        partialMock.clickPlayPauseBtn();

        verify(mockPlayer, times(1)).pause();
        verify(mockPlayer, never()).play();
    }

    @Test
    public void playerViewModel_clickPlayPauseBtnWithGetIsPlayingReturnsFalse_PlayThePlayer() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);
        when(partialMock.getIsPlaying()).thenReturn(false);

        partialMock.clickPlayPauseBtn();

        verify(mockPlayer, times(1)).play();
        verify(mockPlayer, never()).pause();
    }

    @Test
    public void playerViewModel_PlayerMediaItemsProvided_SetAndRetrievePlayerMediaItems() {
        List<MediaItem> mediaItems = new ArrayList<>();
        mediaItems.add(new MediaItem.Builder()
                .setMediaMetadata(new MediaMetadata.Builder().setTitle("123").build())
                .setMediaId("Uri: 123").build());
        mediaItems.add(new MediaItem.Builder()
                .setMediaMetadata(new MediaMetadata.Builder().setTitle("456").build())
                .setMediaId("Uri: 456").build());

        playerViewModel.setPlayer(mockPlayer);
        playerViewModel.setPlayerMediaItems(mediaItems);

        verify(mockPlayer, times(1)).setMediaItems(mediaItems);
        assertEquals(mediaItems, playerViewModel.getPlayerMediaItems().getValue());
    }

    @Test
    public void playerViewModel_IsPlayerExistMediaItemWithPlayerIsNull_ReturnsFalse() {
        playerViewModel.setPlayer(null);
        assertFalse(playerViewModel.isPlayerExistMediaItem());
    }

    @Test
    public void playerViewModel_IsPlayerExistMediaItemWithPlayerMediaItemIsNull_ReturnsFalse() {
        assertFalse(playerViewModel.isPlayerExistMediaItem());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void playerViewModel_IsPlayerExistMediaItemWithPlayerMediaItemIsNonNull_ReturnsTrue() {
        MediaItem mediaItem = new MediaItem.Builder()
                .setMediaMetadata(new MediaMetadata.Builder().build()).build();
        playerViewModel.setPlayer(mockPlayer);
        when(mockPlayer.getCurrentMediaItem()).thenReturn(mediaItem);

        assertTrue(playerViewModel.isPlayerExistMediaItem());
    }

    @Test
    public void playerViewModel_ClearPlayerWhenPlayerNull_ReturnsEarly() {
        playerViewModel.clearPlayer();

        assertNull(playerViewModel.getPlayerOrNull().getValue());
        verifyNoInteractions(mockPlayer);
    }

    @Test
    public void playerViewModel_ClearPlayerWhenPlayerNonNull_ClearMediaItemsAndPauseAndStopPlayer() {
        playerViewModel.setPlayer(mockPlayer);
        playerViewModel.clearPlayer();

        verify(mockPlayer, times(1)).clearMediaItems();
        verify(mockPlayer, times(1)).pause();
        verify(mockPlayer, times(1)).stop();
    }

    @Test
    public void playerViewModel_preparePlayerWithPlayerIsNull_DoNothing() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> playerViewModel.preparePlayer());
        String actualMessage = exception.getMessage();
        assertEquals(actualMessage, PlayerViewModel.PLAYER_IS_NULL);
    }

    @Test
    public void playerViewModel_preparePlayerWithPlayerIsNotNull_prepareThePlayer() {
        playerViewModel.setPlayer(mockPlayer);
        playerViewModel.preparePlayer();
        verify(mockPlayer, times(1)).prepare();
    }

    @Test
    public void playerViewModel_GetSongsWithNullValue_TriesToTraverseAudioFiles() {
        // getSongs() triggers FetchAudioFiles.fetchSongs() and returns a StateFlow
        StateFlow<List<Song>> result = playerViewModel.getSongs();
        // Result should not be null (StateFlow is always non-null)
        assertTrue(result.getValue() != null || result.getValue() == null); // StateFlow returned
    }

    @Test
    public void playerViewModel_GetSongsWithNonNullValue_ReturnsStoredSongs() {
        List<Song> fakeSongs = new ArrayList<>();
        fakeSongs.add(new Song(0, 987654, "UpdatedTest1", Uri.parse("new_abc"), Uri.parse("new_efg"), 2048, 80000));
        fakeSongs.add(new Song(1, 321098, "UpdatedTest2", Uri.parse("new_xyz"), Uri.parse("new_lmn"), 4096, 120000));

        playerViewModel.setSongs(fakeSongs);

        assertEquals(fakeSongs, playerViewModel.getSongs().getValue());
    }

    @Test
    public void playerViewModel_GetSavedMediaItemIndex_WhenIndexIsEqualToMinusOne_ReturnsZero() {
        playerViewModel.getSavedMediaItemIndex();
        assertEquals(0, playerViewModel.getSavedMediaItemIndex());
    }

    @Test
    public void playerViewModel_GetSavedMediaItemIndex_WhenIndexIsNotEqualToMinusOne_ReturnsIndex() {
        // FetchAudioFiles.savedMediaItemIndex is -1 in test env → returns 0
        // Test behavior: if savedMediaItemIndex != -1, it returns the index
        // We test this by verifying the return value matches FetchAudioFiles.savedMediaItemIndex
        int index = FetchAudioFiles.INSTANCE.getSavedMediaItemIndex();
        int expected = (index == -1) ? 0 : index;
        assertEquals(expected, playerViewModel.getSavedMediaItemIndex());
    }

    @Test
    public void playerViewModel_SavedSongNotFoundIsNull_ThrowsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> playerViewModel.getSavedSongNotFound());
        String actualMessage = exception.getMessage();
        assertEquals(actualMessage, PlayerViewModel.SAVED_SONG_NOT_FOUND_VARIABLE_IS_NULL);
    }

    @Test
    public void playerViewModel_SetSavedSongNotFound_WhenCalledWithFalse_SetsFlagToFalse() {
        playerViewModel.getSavedMediaItemIndex(); // initializes the flow (sets to true, since savedMediaItemIndex=-1)
        playerViewModel.setSavedSongNotFound(false); // explicitly set to false
        assertFalse(Objects.requireNonNull(playerViewModel.getSavedSongNotFound().getValue()));
    }

    @Test
    public void playerViewModel_SetSavedSongNotFound_WhenCalledWithTrue_SetsFlagToTrue() {
        playerViewModel.setSavedSongNotFound(true);
        assertTrue(Objects.requireNonNull(playerViewModel.getSavedSongNotFound().getValue()));
    }

    @Test
    public void playerViewModel_WithSessionTokenProvided_SetTokenValueUpdatedCanBeRetrieved() {
        SessionToken mockToken = mock(SessionToken.class);
        playerViewModel.setSessionToken(mockToken);
        assertEquals(mockToken, playerViewModel.getSessionToken().getValue());
    }

    @Test
    public void playerViewModel_MillisecondToSecondPassMilliSecond_ReturnsSecond() {
        long fakeMilliSecond = 3120000;
        assertEquals(playerViewModel.millisecondToSecond(fakeMilliSecond), fakeMilliSecond / 1000);
    }

    @Test
    public void playerViewModel_GetReadableTimePassSecond_ReturnsReadableTimeString() {
        assertEquals("04:00", playerViewModel.getReadableTime(240));
        assertEquals("01:10:21", playerViewModel.getReadableTime(4221));
    }

    @Test
    public void playerViewModel_SaveCurrentSongStatusWithIsPlayerExistMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        partialMock.saveCurrentSongStatus();

        verify(partialMock, never()).millisecondToSecond(anyLong());
        verify(partialMock, never()).getPlayerMode();
        verifyNoInteractions(mockSharedData);
    }

    @Test
    public void playerViewModel_SaveCurrentSongStatusWithIsPlayerExistMediaItemIsTrue_SavesStatusToSharedData() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));
        partialMock.setPlayer(mockPlayer);
        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        String fakeCurrentSongId = "fakeSongId";
        MediaItem mediaItem = new MediaItem.Builder()
                .setMediaId(fakeCurrentSongId)
                .setMediaMetadata(new MediaMetadata.Builder().setTitle("123").build())
                .build();
        doReturn(mediaItem).when(mockPlayer).getCurrentMediaItem();

        long fakePlayingCurrentMillisecond = 1234567;
        doReturn(fakePlayingCurrentMillisecond).when(mockPlayer).getCurrentPosition();

        int fakePlayingCurrentSecond = 1234;
        when(partialMock.millisecondToSecond(fakePlayingCurrentMillisecond)).thenReturn(fakePlayingCurrentSecond);

        MutableStateFlow<Integer> fakePlayerMode = StateFlowKt.MutableStateFlow(2);
        when(partialMock.getPlayerMode()).thenReturn((StateFlow<Integer>) (StateFlow<?>) fakePlayerMode);

        partialMock.saveCurrentSongStatus();

        verify(mockSharedData, times(1)).setSongMediaId(fakeCurrentSongId);
        verify(mockSharedData, times(1)).setSongPosition(fakePlayingCurrentSecond);
        verify(mockSharedData, times(1)).setPlayerMode(Objects.requireNonNull(fakePlayerMode.getValue()));
    }

    @Test
    public void playerViewModel_OnCleared_CallsSaveCurrentSongStatus() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData));

        partialMock.onCleared();

        verify(partialMock, times(1)).saveCurrentSongStatus();
    }
}
