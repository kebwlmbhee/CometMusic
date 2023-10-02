package com.example.cometmusic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.media3.session.MediaController;

import com.example.cometmusic.model.FetchAudioFiles;
import com.example.cometmusic.model.SharedData;
import com.example.cometmusic.viewmodel.PlayerViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.Objects;

public class PlayerViewModelTest {

    // force LiveData from multi-thread to single thread
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private PlayerViewModel viewModel;

    @Mock
    private Application mockApplication;

    @Mock
    private SharedData mockSharedData;

    @Mock
    private FetchAudioFiles fetchAudioFiles;

    @Mock
    private Observer<String> songNameObserver;

    @Mock
    MutableLiveData<MediaController> mockPlayerLiveData = new MutableLiveData<>();

    @Mock
    MutableLiveData<Integer> playerModeLiveData = new MutableLiveData<>();

    @Mock
    MediaController mockPlayer = mock(MediaController.class);

    @Before
    public void setUp() {
        // initialize the object with Mock Tag
        MockitoAnnotations.openMocks(this);

        when(mockApplication.getApplicationContext()).thenReturn(mockApplication);

        mockPlayerLiveData.setValue(mockPlayer);

        // keep original behavior
        mockSharedData = mock(SharedData.class);
        fetchAudioFiles = spy(FetchAudioFiles.getInstance(mockApplication.getApplicationContext()));

        viewModel = spy(new PlayerViewModel((Application) mockApplication.getApplicationContext(), mockSharedData));
        viewModel.getCurrentSongName().observeForever(songNameObserver);
    }

    @Test
    public void playerViewModel_WhenPlayerNotSetOrIsNull_ThrowIllegalStateException() {

        // assert it will throw the IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> viewModel.getPlayer());

        // verify the exception message
        String actualMessage = exception.getMessage();
        assertEquals(actualMessage, PlayerViewModel.playerIsNullException);
    }

    @Test
    public void playerViewModel_WithPlayerProvided_SetPlayerValueUpdatedCanBeRetrieved() {
        // call the method that should be tested
        viewModel.setPlayer(mockPlayer);

        assertEquals(mockPlayer, viewModel.getPlayer().getValue());
    }

    @Test
    public void playerViewModel_WhenPlayerModeIsNull_ReturnsSharedDataPlayerMode() {

        // set the sharedData getPlayerMode value to 1
        when(mockSharedData.getPlayerMode()).thenReturn(1);

        // call the method that should be tested
        playerModeLiveData = viewModel.getPlayerMode();

        assertEquals((Object) playerModeLiveData.getValue(), 1);
    }

    @Test
    public void playerViewModel_WhenPlayerModeIsOne_ShouldSetRepeatAll() {
        viewModel.setPlayer(mockPlayer);

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
        viewModel.setPlayerMode(1);

        // get updated playerMode
        playerModeLiveData = viewModel.getPlayerMode();

        // 2 is represent REPEAT_MODE_ALL
        assertEquals((Object) repeatModeCaptor.getValue(), 2);

        // shuffleMode is false
        assertFalse(shuffleModeCaptor.getValue());

        // assert the player mode is 1
        assertEquals(Objects.requireNonNull(playerModeLiveData.getValue()).intValue(), 1);
    }

    @Test
    public void playerViewModel_WhenPlayerModeIsTwo_ShouldSetRepeatOne() {
        viewModel.setPlayer(mockPlayer);

        // get setRepeatMode parameter and store into Captor
        ArgumentCaptor<Integer> repeatModeCaptor = ArgumentCaptor.forClass(Integer.class);

        // get setRepeatMode parameter and store into Captor
        Mockito.doAnswer((Answer<Void>) invocation -> null)
                .when(mockPlayer).setRepeatMode(repeatModeCaptor.capture());

        // initially is null
        // call the method that should be tested
        viewModel.setPlayerMode(2);

        // get updated playerMode
        playerModeLiveData = viewModel.getPlayerMode();

        // 1 is represent REPEAT_MODE_ONE
        assertEquals((Object) repeatModeCaptor.getValue(), 1);

        // assert the player mode is 2
        assertEquals(Objects.requireNonNull(playerModeLiveData.getValue()).intValue(), 2);
    }

    @Test
    public void playerViewModel_WhenPlayerModeIsThree_ShouldSetRandomAll() {
        viewModel.setPlayer(mockPlayer);

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
        viewModel.setPlayerMode(3);

        // get updated playerMode
        MutableLiveData<Integer> playerModeLiveData = viewModel.getPlayerMode();

        // 0 is represent REPEAT_MODE_OFF
        assertEquals((Object) repeatModeCaptor.getValue(), 0);

        // shuffleMode is true
        assertTrue(shuffleModeCaptor.getValue());

        // assert the player mode is 3
        assertEquals(Objects.requireNonNull(playerModeLiveData.getValue()).intValue(), 3);
    }

}
