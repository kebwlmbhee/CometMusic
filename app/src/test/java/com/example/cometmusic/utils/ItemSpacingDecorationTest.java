package com.example.cometmusic.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cometmusic.ui.decoration.ItemSpacingDecoration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;


@RunWith(AndroidJUnit4.class)
@Config(sdk = 33)
public class ItemSpacingDecorationTest {
    private ItemSpacingDecoration itemSpacingDecoration;

    @Mock
    private View view;

    @Mock
    private RecyclerView parent;

    @Mock
    private RecyclerView.State state;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        itemSpacingDecoration = new ItemSpacingDecoration();
    }

    @Test
    public void itemSpacingDecoration_PositionExist_setItemBottomSpace() {
        Rect outRect = new Rect();

        // mock method inside super
        RecyclerView.LayoutParams layoutParams = mock(RecyclerView.LayoutParams.class);
        doReturn(layoutParams).when(view).getLayoutParams();
        doReturn(123).when(layoutParams).getViewLayoutPosition();

        doReturn(0).when(parent).getChildAdapterPosition(view);

        // call the method that should be tested
        itemSpacingDecoration.getItemOffsets(outRect, view, parent, state);

        assertEquals(outRect.bottom, ItemSpacingDecoration.itemBottomSpace);
    }

    @Test
    public void itemSpacingDecoration_PositionExist_NotToSetItemBottomSpace() {
        Rect outRect = new Rect();

        // mock method inside super
        RecyclerView.LayoutParams layoutParams = mock(RecyclerView.LayoutParams.class);
        doReturn(layoutParams).when(view).getLayoutParams();
        doReturn(123).when(layoutParams).getViewLayoutPosition();

        doReturn(RecyclerView.NO_POSITION).when(parent).getChildAdapterPosition(view);

        // call the method that should be tested
        itemSpacingDecoration.getItemOffsets(outRect, view, parent, state);

        System.out.println("123: " + outRect.bottom);
        assertNotEquals(outRect.bottom, ItemSpacingDecoration.itemBottomSpace);
    }
}
