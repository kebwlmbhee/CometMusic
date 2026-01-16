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

import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.test.core.app.ApplicationProvider;

import com.example.cometmusic.R;
import com.example.cometmusic.model.FetchAudioFiles;
import com.example.cometmusic.model.SharedData;
import com.example.cometmusic.viewmodel.PlayerViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
        // initialize the object with Mock Tag
        MockitoAnnotations.openMocks(this);

        mockSharedData = mock(SharedData.class);
        mockFetchAudioFiles = mock(FetchAudioFiles.class);
        mockPlayer = mock(MediaController.class);

        playerViewModel = new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles);
    }

    @Test
    public void playerViewModel_WhenPlayerIsNull_ThrowsIllegalStateException() {

        // assert it will throw the IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> playerViewModel.getPlayer());

        // assert the exception message
        String actualMessage = exception.getMessage();
        assertEquals(actualMessage, PlayerViewModel.playerIsNullException);
    }

    @Test
    public void playerViewModel_SetPlayerWithPlayerProvided_SetPlayerUpdatedCanBeRetrieved() {
        // call the method that should be tested
        playerViewModel.setPlayer(mockPlayer);

        assertEquals(mockPlayer, playerViewModel.getPlayer().getValue());
    }

    @Test
    public void playerViewModel_GetPlayerModeWithPlayerModeIsNull_ReturnsSharedDataPlayerMode() {

        // set the sharedData getPlayerMode value to 2
        when(mockSharedData.getPlayerMode()).thenReturn(2);

        // call the method that should be tested
        assertEquals((Object) playerViewModel.getPlayerMode().getValue(), 2);
    }

    @Test
    public void playerViewModel_WhenPlayerModeIsOne_ShouldSetRepeatAll() {
        playerViewModel.setPlayer(mockPlayer);

        // mock setRepeatMode
        ArgumentCaptor<Integer> repeatModeCaptor = ArgumentCaptor.forClass(Integer.class);

        // get setRepeatMode parameter and store into Captor
        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setRepeatMode(repeatModeCaptor.capture());

        // mock setShuffleMode
        ArgumentCaptor<Boolean> shuffleModeCaptor = ArgumentCaptor.forClass(Boolean.class);

        // get setShuffleMode parameter and store into Captor
        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setShuffleModeEnabled(shuffleModeCaptor.capture());

        // initially is null
        // call the method that should be tested
        playerViewModel.setPlayerMode(1);

        // 2 is represent REPEAT_MODE_ALL
        assertEquals((Object) repeatModeCaptor.getValue(), 2);

        // shuffleMode is false
        assertFalse(shuffleModeCaptor.getValue());

        // assert the player mode is 1
        assertEquals((Object) playerViewModel.getPlayerMode().getValue(), 1);
    }

    @Test
    public void playerViewModel_WhenPlayerModeIsTwo_ShouldSetRepeatOne() {
        playerViewModel.setPlayer(mockPlayer);

        // get setRepeatMode parameter and store into Captor
        ArgumentCaptor<Integer> repeatModeCaptor = ArgumentCaptor.forClass(Integer.class);

        // get setRepeatMode parameter and store into Captor
        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setRepeatMode(repeatModeCaptor.capture());

        // initially is null
        // call the method that should be tested
        playerViewModel.setPlayerMode(2);

        // 1 is represent REPEAT_MODE_ONE
        assertEquals((Object) repeatModeCaptor.getValue(), 1);

        // assert the player mode is 2
        assertEquals((Object) playerViewModel.getPlayerMode().getValue(), 2);
    }

    @Test
    public void playerViewModel_WhenPlayerModeIsThree_ShouldSetRandomAll() {
        playerViewModel.setPlayer(mockPlayer);

        // mock setRepeatMode
        ArgumentCaptor<Integer> repeatModeCaptor = ArgumentCaptor.forClass(Integer.class);

        // get setRepeatMode parameter and store into Captor
        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setRepeatMode(repeatModeCaptor.capture());

        // mock setShuffleMode
        ArgumentCaptor<Boolean> shuffleModeCaptor = ArgumentCaptor.forClass(Boolean.class);

        // get setShuffleMode parameter and store into Captor
        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setShuffleModeEnabled(shuffleModeCaptor.capture());

        // initially is null
        // call the method that should be tested
        playerViewModel.setPlayerMode(3);

        // get updated playerMode
        MutableLiveData<Integer> playerModeLiveData = playerViewModel.getPlayerMode();

        // 0 is represent REPEAT_MODE_OFF
        assertEquals((Object) repeatModeCaptor.getValue(), 0);

        // shuffleMode is true
        assertTrue(shuffleModeCaptor.getValue());

        // assert the player mode is 3
        assertEquals(Objects.requireNonNull(playerModeLiveData.getValue()).intValue(), 3);
    }

    @Test
    public void playerViewModel_ClickRepeatButtonWithPlayerMode_SetPlayerModeToNext() {
        playerViewModel.setPlayer(mockPlayer);

        playerViewModel.setPlayerMode(1);

        playerViewModel.clickRepeatButton();

        assertEquals((Object) playerViewModel.getPlayerMode().getValue(), 2);

        playerViewModel.clickRepeatButton();

        assertEquals((Object) playerViewModel.getPlayerMode().getValue(), 3);

        playerViewModel.clickRepeatButton();

        assertEquals((Object) playerViewModel.getPlayerMode().getValue(), 1);
    }


    @Test
    public void playerViewModel_SkipToNextSongWithIsPlayerExistMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        // call the method that should be tested
        partialMock.skipToNextSong();

        verify(mockPlayer, never()).seekToNext();
    }

    @Test
    public void playerViewModel_SkipToNextSongWithHasNextMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        when(mockPlayer.hasNextMediaItem()).thenReturn(false);

        // call the method that should be tested
        partialMock.skipToNextSong();

        verify(mockPlayer, never()).seekToNext();
    }

    @Test
    public void playerViewModel_SkipToNextSongWithConditionCorresponded_CallThePlayerSeekToNext() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        when(mockPlayer.hasNextMediaItem()).thenReturn(true);

        // call the method that should be tested
        partialMock.skipToNextSong();

        verify(mockPlayer, times(1)).seekToNext();
    }

    @Test
    public void playerViewModel_SkipToPreviousSongWithIsPlayerExistMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        // call the method that should be tested
        partialMock.skipToPreviousSong();

        verify(mockPlayer, never()).seekToPrevious();
    }

    @Test
    public void playerViewModel_SkipToPreviousSongWithHasPreviousMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        when(mockPlayer.hasPreviousMediaItem()).thenReturn(false);

        // call the method that should be tested
        partialMock.skipToPreviousSong();

        verify(mockPlayer, never()).seekToPrevious();
    }

    @Test
    public void playerViewModel_SkipToPreviousSongWithConditionCorresponded_CallThePlayerSeekToPrevious() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        when(mockPlayer.hasPreviousMediaItem()).thenReturn(true);

        // call the method that should be tested
        partialMock.skipToPreviousSong();

        verify(mockPlayer, times(1)).seekToPrevious();
    }

    @Test
    public void playerViewModel_GetPlayerCurrentIndexWithIsPlayerExistMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));
        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        // call the method that should be tested
        assertEquals(partialMock.getPlayerCurrentIndex(), 0);
    }

    @Test
    public void playerViewModel_GetPlayerCurrentIndexWithIsPlayerExistMediaItemIsTrue_ReturnsCurrentMediaItemIndex() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        int fakeCurrentMediaItemIndex = 101;

        doReturn(fakeCurrentMediaItemIndex)
                .when(mockPlayer)
                .getCurrentMediaItemIndex();

        // call the method that should be tested
        assertEquals(partialMock.getPlayerCurrentIndex(), fakeCurrentMediaItemIndex);
    }

    @Test
    public void playerViewModel_GetDurationSecondWithDurationSecondIsNull_ReturnsDefaultValue() {
        // call the method that should be tested
        assertEquals((Object) playerViewModel.getDurationSecond().getValue(), 100);
    }

    @Test
    public void playerViewModel_SetDurationSecondWithIsPlayerExistMediaItemIsFalse_ResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        // call the method that should be tested
        partialMock.setDurationSecond();

        verify(partialMock, never()).getReadableTime(anyInt());
    }

    @Test
    public void playerViewModel_SetDurationSecondWithIsPlayerExistMediaItemIsTrue_ResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        long[] fakeDurationMillisecond = {40000, 60000};

        List<Song> fakeSongs = new ArrayList<>();

        MutableLiveData<List<Song>> fakeLiveDataSongs = new MutableLiveData<>();

        fakeSongs.add(new Song(0, 123456, "Test1", Uri.parse("abc"), Uri.parse("efg"), 1024, fakeDurationMillisecond[0]));
        fakeSongs.add(new Song(1, 789012, "Test2", Uri.parse("xyz"), Uri.parse("lmn"), 2048, fakeDurationMillisecond[1]));

        fakeLiveDataSongs.setValue(fakeSongs);

        when(partialMock.getSongs()).thenReturn(fakeLiveDataSongs);

        int fakeCurrentMediaItemIndex = 0;

        doReturn(fakeCurrentMediaItemIndex)
                .when(mockPlayer)
                .getCurrentMediaItemIndex();

        // call the method that should be tested
        partialMock.setDurationSecond();

        int fakeDurationSecond = partialMock.millisecondToSecond(fakeDurationMillisecond[fakeCurrentMediaItemIndex]);

        assertEquals((Object) partialMock.getDurationSecond().getValue(), fakeDurationSecond);
        assertEquals(partialMock.getReadableDurationString().getValue(), partialMock.getReadableTime(fakeDurationSecond));

        // one more again
        fakeCurrentMediaItemIndex = 1;

        doReturn(fakeCurrentMediaItemIndex)
                .when(mockPlayer)
                .getCurrentMediaItemIndex();

        // call the method that should be tested
        partialMock.setDurationSecond();

        fakeDurationSecond = partialMock.millisecondToSecond(fakeDurationMillisecond[fakeCurrentMediaItemIndex]);

        assertEquals((Object) partialMock.getDurationSecond().getValue(), fakeDurationSecond);
        assertEquals(partialMock.getReadableDurationString().getValue(), partialMock.getReadableTime(fakeDurationSecond));

    }

    @Test
    public void playerViewModel_GetCurrentSecondWithCurrentSecondIsNull_ReturnsDefaultValue() {
        assertEquals((Object) playerViewModel.getCurrentSecond().getValue(), -1);
    }

    @Test
    public void playerViewModel_SetCurrentSecondWithIsPlayerExistMediaItemIsFalse_DefaultResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(ApplicationProvider.getApplicationContext(), mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        // call the method that should be tested
        partialMock.setCurrentSecond();

        Toast toast = ShadowToast.getLatestToast();

        assertEquals(toast.getDuration(), Toast.LENGTH_SHORT);

        String expectedText = ApplicationProvider.getApplicationContext().getString(R.string.can_not_find_current_second);

        String actualText = ShadowToast.getTextOfLatestToast();

        assertEquals(expectedText, actualText);

        assertEquals((Object) partialMock.getCurrentSecond().getValue(), -1);
        assertEquals(partialMock.getReadableCurrentString().getValue(), partialMock.getReadableTime(-1));
    }

    @Test
    public void playerViewModel_SetCurrentSecondWithIsPlayerExistMediaItemIsTrue_ResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        long fakeCurrentMillisecond = 800;

        when(mockPlayer.getCurrentPosition()).thenReturn(fakeCurrentMillisecond);

        // call the method that should be tested
        partialMock.setCurrentSecond();

        int fakeCurrentSecond = partialMock.millisecondToSecond(fakeCurrentMillisecond);

        assertEquals((Object) partialMock.getCurrentSecond().getValue(), fakeCurrentSecond);
        assertEquals(partialMock.getReadableCurrentString().getValue(), partialMock.getReadableTime(fakeCurrentSecond));
    }

    @Test
    public void playerViewModel_GetReadableCurrentStringWithReadableCurrentStringIsNull_CallSetCurrentSecond() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        doNothing().when(partialMock).setCurrentSecond();

        // call the method that should be tested
        partialMock.getReadableCurrentString();

        verify(partialMock, times(1)).setCurrentSecond();
    }

    @Test
    public void playerViewModel_GetReadableDurationStringWithReadableDurationStringIsNull_CallSetDurationSecond() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        doNothing().when(partialMock).setDurationSecond();

        // call the method that should be tested
        partialMock.getReadableDurationString();

        verify(partialMock, times(1)).setDurationSecond();
    }

    @Test
    public void playerViewModel_SeekToPosition_ResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        // call the method that should be tested
        partialMock.seekToPosition(300);

        verify(mockPlayer, times(1)).seekTo(300);
    }

    @Test
    public void playerViewModel_SeekToSongIndexAndPosition_ResultIsSet() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        doNothing().when(partialMock).setCurrentSecond();

        // call the method that should be tested
        partialMock.seekToSongIndexAndPosition(100, 200);

        verify(mockPlayer, times(1)).seekTo(100, 200);

        verify(partialMock, times(1)).setCurrentSecond();
    }

    @Test
    public void playerViewModel_SetSecondAndStringWhenMoving_ReturnValue() {

        // call the method that should be tested
        playerViewModel.setSecondAndStringWhenMoving(100);

        assertEquals((Object) playerViewModel.getCurrentSecond().getValue(), 100);

        assertEquals(playerViewModel.getReadableCurrentString().getValue(), playerViewModel.getReadableTime(100));
    }

    @Test
    public void playerViewModel_GetCurrentSongNameWithCurrentSongNameIsNull_CallsSetCurrentSongName() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        // call the method that should be tested
        assertNull(partialMock.getCurrentSongName().getValue());

        verify(partialMock, times(1)).setCurrentSongName();
    }

    @Test
    public void playerViewModel_GetCurrentSongNameWithCurrentSongNameProvided_ReturnsValue() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        String fakeName = "mockSongName";

        MediaItem mediaItem = new MediaItem.Builder()
                .setMediaMetadata(new MediaMetadata.Builder().setTitle(fakeName).build())
                .build();

        doReturn(mediaItem)
                .when(mockPlayer)
                .getCurrentMediaItem();

        System.out.println(mediaItem.mediaMetadata.title);

        // call the method that should be tested
        partialMock.setCurrentSongName();

        // call the method that should be tested
        assertEquals(partialMock.getCurrentSongName().getValue(), fakeName);
    }

    @Test
    public void playerViewModel_clickPlayPauseBtnWithIsPlayerExistMediaItemFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        partialMock.setPlayer(mockPlayer);

        // call the method that should be tested
        partialMock.clickPlayPauseBtn();

        verifyNoInteractions(mockPlayer);

        verify(partialMock, never()).getIsPlaying();
    }

    @Test
    public void playerViewModel_clickPlayPauseBtnWithGetIsPlayingReturnsTrue_PausesThePlayer() {

        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        when(partialMock.getIsPlaying()).thenReturn(true);

        // call the method that should be tested
        partialMock.clickPlayPauseBtn();

        verify(mockPlayer, times(1)).pause();

        verify(mockPlayer, never()).play();
    }

    @Test
    public void playerViewModel_clickPlayPauseBtnWithGetIsPlayingReturnsFalse_PlayThePlayer() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        when(partialMock.getIsPlaying()).thenReturn(false);

        // call the method that should be tested
        partialMock.clickPlayPauseBtn();

        verify(mockPlayer, times(1)).play();

        verify(mockPlayer, never()).pause();
    }

    @Test
    public void playerViewModel_PlayerMediaItemsProvided_SetAndRetrievePlayerMediaItems() {

        List<MediaItem> mediaItems = new ArrayList<>();

        mediaItems.add(new MediaItem.Builder()
                .setMediaMetadata
                        (new MediaMetadata.Builder()
                                .setTitle("123")
                                .build())
                .setMediaId("Uri: 123")
                .build());

        mediaItems.add(new MediaItem.Builder()
                .setMediaMetadata
                        (new MediaMetadata.Builder()
                                .setTitle("456")
                                .build())
                .setMediaId("Uri: 456")
                .build());

        playerViewModel.setPlayer(mockPlayer);

        // call the method that should be tested
        playerViewModel.setPlayerMediaItems(mediaItems);

        verify(mockPlayer, times(1)).setMediaItems(mediaItems);

        // call the method that should be tested
        assertEquals(mediaItems, playerViewModel.getPlayerMediaItems().getValue());
    }

    @Test
    public void playerViewModel_IsPlayerExistMediaItemWithPlayerIsNull_ReturnsFalse() {
        playerViewModel.setPlayer(null);

        // call the method that should be tested
        assertFalse(playerViewModel.isPlayerExistMediaItem());
    }

    @Test
    public void playerViewModel_IsPlayerExistMediaItemWithPlayerMediaItemIsNull_ReturnsFalse() {
        // call the method that should be tested
        assertFalse(playerViewModel.isPlayerExistMediaItem());
    }

    // remove NonNull notation warning
    @SuppressWarnings("ConstantConditions")
    @Test
    public void playerViewModel_IsPlayerExistMediaItemWithPlayerMediaItemIsNonNull_ReturnsTrue() {

        MediaItem mediaItem = new MediaItem.Builder()
                .setMediaMetadata(new MediaMetadata.Builder().build())
                .build();

        playerViewModel.setPlayer(mockPlayer);

        when(mockPlayer.getCurrentMediaItem()).thenReturn(mediaItem);

        // call the method that should be tested
        assertTrue(playerViewModel.isPlayerExistMediaItem());
    }

    @Test
    public void playerViewModel_ClearPlayerWhenPlayerNull_ReturnsEarly() {

        // call the method that should be tested
        playerViewModel.clearPlayer();

        assertNull(playerViewModel.getPlayerOrNull().getValue());

        // verify that player's related operations were not called
        verifyNoInteractions(mockPlayer);
    }

    @Test
    public void playerViewModel_ClearPlayerWhenPlayerNonNull_ClearMediaItemsAndPauseAndStopPlayer() {

        playerViewModel.setPlayer(mockPlayer);

        // call the method that should be tested
        playerViewModel.clearPlayer();

        verify(mockPlayer, times(1)).clearMediaItems();

        verify(mockPlayer, times(1)).pause();

        verify(mockPlayer, times(1)).stop();
    }

    @Test
    public void playerViewModel_preparePlayerWithPlayerIsNull_DoNothing() {

        // assert it will throw the IllegalStateException
        // call the method that should be tested
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> playerViewModel.preparePlayer());

        // assert the exception message
        String actualMessage = exception.getMessage();
        assertEquals(actualMessage, PlayerViewModel.playerIsNullException);
    }

    @Test
    public void playerViewModel_preparePlayerWithPlayerIsNotNull_prepareThePlayer() {

        playerViewModel.setPlayer(mockPlayer);

        // call the method that should be tested
        playerViewModel.preparePlayer();

        verify(mockPlayer, times(1)).prepare();
    }


    @Test
    public void playerViewModel_GetSongsWithNullValue_TriesToTraverseAudioFiles() {
        List<Song> fakeSongs = new ArrayList<>();

        fakeSongs.add(new Song(0, 123456, "Test1", Uri.parse("abc"), Uri.parse("efg"), 1024, 40000));
        fakeSongs.add(new Song(1, 789012, "Test2", Uri.parse("xyz"), Uri.parse("lmn"), 2048, 60000));

        when(mockFetchAudioFiles.getSongs()).thenReturn(fakeSongs);

        // call the method that should be tested
        assertEquals(fakeSongs, playerViewModel.getSongs().getValue());
    }


    @Test
    public void playerViewModel_GetSongsWithNonNullValue_ReturnsStoredSongs() {
        List<Song> fakeSongs = new ArrayList<>();

        fakeSongs.add(new Song(0, 987654, "UpdatedTest1", Uri.parse("new_abc"), Uri.parse("new_efg"), 2048, 80000));
        fakeSongs.add(new Song(1, 321098, "UpdatedTest2", Uri.parse("new_xyz"), Uri.parse("new_lmn"), 4096, 120000));


        playerViewModel.setSongs(fakeSongs);

        // call the method that should be tested
        assertEquals(fakeSongs, playerViewModel.getSongs().getValue());
    }

    @Test
    public void playerViewModel_GetSavedMediaItemIndex_WhenIndexIsEqualToMinusOne_ReturnsZero() {
        when(mockFetchAudioFiles.getSavedMediaItemIndex()).thenReturn(-1);

        // call the method that should be tested
        playerViewModel.getSavedMediaItemIndex();

        assertEquals(playerViewModel.getSavedMediaItemIndex(), 0);
    }

    @Test
    public void playerViewModel_GetSavedMediaItemIndex_WhenIndexIsNotEqualToMinusOne_ReturnsIndex() {
        when(mockFetchAudioFiles.getSavedMediaItemIndex()).thenReturn(10);

        // call the method that should be tested
        playerViewModel.getSavedMediaItemIndex();

        assertEquals(playerViewModel.getSavedMediaItemIndex(), 10);
    }

    @Test
    public void playerViewModel_SavedSongNotFoundIsNull_ThrowsIllegalStateException() {

        // assert it will throw the IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> playerViewModel.getSavedSongNotFound());

        // assert the exception message
        String actualMessage = exception.getMessage();
        assertEquals(actualMessage, PlayerViewModel.savedSongNotFoundVariableIsNull);
    }

    @Test
    public void playerViewModel_SetSavedSongNotFound_WhenCalledWithFalse_SetsFlagToFalse() {
        when(mockFetchAudioFiles.getSavedMediaItemIndex()).thenReturn(2);

        playerViewModel.getSavedMediaItemIndex();

        // call the method that should be tested
        assertFalse(Objects.requireNonNull(playerViewModel.getSavedSongNotFound().getValue()));
    }

    @Test
    public void playerViewModel_SetSavedSongNotFound_WhenCalledWithTrue_SetsFlagToTrue() {
        when(mockFetchAudioFiles.getSavedMediaItemIndex()).thenReturn(-1);

        playerViewModel.getSavedMediaItemIndex();

        // call the method that should be tested
        assertTrue(Objects.requireNonNull(playerViewModel.getSavedSongNotFound().getValue()));
    }

    @Test
    public void playerViewModel_WithSessionTokenProvided_SetTokenValueUpdatedCanBeRetrieved() {
        SessionToken mockToken = mock(SessionToken.class);

        // call the method that should be tested
        playerViewModel.setSessionToken(mockToken);

        assertEquals(mockToken, playerViewModel.getSessionToken().getValue());
    }

    @Test
    public void playerViewModel_MillisecondToSecondPassMilliSecond_ReturnsSecond() {
        long fakeMilliSecond = 3120000;

        // call the method that should be tested
        assertEquals(playerViewModel.millisecondToSecond(fakeMilliSecond), fakeMilliSecond / 1000);
    }

    @Test
    public void playerViewModel_GetReadableTimePassSecond_ReturnsReadableTimeString() {
        int fakeSecond = 240;

        // call the method that should be tested
        assertEquals(playerViewModel.getReadableTime(fakeSecond), "04:00");

        fakeSecond = 4221;

        // call the method that should be tested
        assertEquals(playerViewModel.getReadableTime(fakeSecond), "01:10:21");
    }

    @Test
    public void playerViewModel_SaveCurrentSongStatusWithIsPlayerExistMediaItemIsFalse_ReturnsEarly() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        when(partialMock.isPlayerExistMediaItem()).thenReturn(false);

        // call the method that should be tested
        partialMock.saveCurrentSongStatus();

        verify(partialMock, never()).millisecondToSecond(anyLong());

        verify(partialMock, never()).getPlayerMode();

        verifyNoInteractions(mockSharedData);
    }

    @Test
    public void playerViewModel_SaveCurrentSongStatusWithIsPlayerExistMediaItemIsTrue_SavesStatusToSharedData() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        partialMock.setPlayer(mockPlayer);

        when(partialMock.isPlayerExistMediaItem()).thenReturn(true);

        String fakeCurrentSongId = "fakeSongId";
        MediaItem mediaItem = new MediaItem.Builder()
                .setMediaId(fakeCurrentSongId)
                .setMediaMetadata(new MediaMetadata.Builder().setTitle("123").build())
                .build();
        doReturn(mediaItem)
                .when(mockPlayer)
                .getCurrentMediaItem();

        long fakePlayingCurrentMillisecond = 1234567;
        doReturn(fakePlayingCurrentMillisecond)
                .when(mockPlayer)
                .getCurrentPosition();

        MutableLiveData<Integer> fakePlayerMode = new MutableLiveData<>(2);

        int fakePlayingCurrentSecond = 1234;
        when(partialMock.millisecondToSecond(fakePlayingCurrentMillisecond)).thenReturn(fakePlayingCurrentSecond);

        when(partialMock.getPlayerMode()).thenReturn(fakePlayerMode);

        // call the method that should be tested
        partialMock.saveCurrentSongStatus();

        verify(mockSharedData, times(1)).setSongMediaId(fakeCurrentSongId);
        verify(mockSharedData, times(1)).setSongPosition(fakePlayingCurrentSecond);
        verify(mockSharedData, times(1)).setPlayerMode(Objects.requireNonNull(fakePlayerMode.getValue()));
    }

    @Test
    public void playerViewModel_OnCleared_CallsSaveCurrentSongStatus() {
        PlayerViewModel partialMock = spy(new PlayerViewModel(mockApplication, mockSharedData, mockFetchAudioFiles));

        // call the method that should be tested
        partialMock.onCleared();

        verify(partialMock, times(1)).saveCurrentSongStatus();
    }
}