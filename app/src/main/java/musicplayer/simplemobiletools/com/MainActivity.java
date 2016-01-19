package musicplayer.simplemobiletools.com;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private static final int MIN_DURATION_MS = 20000;
    private ArrayList<Song> songs;
    private MusicService musicService;
    private boolean isMusicBound;

    @Bind(R.id.playPauseBtn) ImageView playPauseBtn;
    @Bind(R.id.songs) ListView songsList;
    @Bind(R.id.songTitle) TextView titleTV;
    @Bind(R.id.songArtist) TextView artistTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        songs = new ArrayList<>();

        getSortedSongs();

        final SongAdapter adapter = new SongAdapter(this, songs);
        songsList.setAdapter(adapter);
        songsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                songPicked(position);
            }
        });

        final Intent intent = new Intent(this, MusicService.class);
        bindService(intent, musicConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    private void songPicked(int pos) {
        musicService.setSong(pos, true);
        updateSongInfo(musicService.getCurrSong());
        setPauseIcon();
    }

    private void updateSongInfo(Song song) {
        if (song != null) {
            titleTV.setText(song.getTitle());
            artistTV.setText(song.getArtist());
        }
    }

    private void getSortedSongs() {
        getSongs();
        Collections.sort(songs, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }

    private void getSongs() {
        final ContentResolver musicResolver = getContentResolver();
        final Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            final int idIndex = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            final int titleIndex = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            final int artistIndex = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            final int durationIndex = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            do {
                if (musicCursor.getInt(durationIndex) > MIN_DURATION_MS) {
                    final long id = musicCursor.getLong(idIndex);
                    final String title = musicCursor.getString(titleIndex);
                    final String artist = musicCursor.getString(artistIndex);
                    songs.add(new Song(id, title, artist));
                }
            } while (musicCursor.moveToNext());
            musicCursor.close();
        }
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            final MusicService.MyBinder binder = (MusicService.MyBinder) iBinder;
            musicService = binder.getService();
            musicService.setSongs(songs);
            isMusicBound = true;

            updateSongInfo(musicService.getCurrSong());
            if (musicService.isPlaying())
                setPauseIcon();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isMusicBound = false;
        }
    };

    @Override
    protected void onDestroy() {
        if (isMusicBound) {
            isMusicBound = false;
            unbindService(musicConnection);
        }

        super.onDestroy();
    }

    @OnClick(R.id.previousBtn)
    public void previousClicked() {
        if (isPlaylistEmpty())
            return;

        playPreviousSong();
    }

    @OnClick(R.id.playPauseBtn)
    public void playPauseClicked() {
        if (isPlaylistEmpty())
            return;

        resumePauseSong();
    }

    @OnClick(R.id.nextBtn)
    public void nextClicked() {
        if (isPlaylistEmpty())
            return;

        playNextSong();
    }

    @OnClick(R.id.stopBtn)
    public void stopClicked() {
        if (isPlaylistEmpty())
            return;

        stopMusic();
    }

    public void stopMusic() {
        if (musicService != null) {
            musicService.stopSong();
            setPlayIcon();
        }
    }

    public void resumePauseSong() {
        if (songs.isEmpty() || musicService == null)
            return;

        if (musicService.isPlaying()) {
            pauseSong();
        } else {
            resumeSong();
        }

        // in case we just launched the app and pressed play, also update the song and artist name
        if (artistTV.getText().toString().trim().isEmpty())
            updateSongInfo(musicService.getCurrSong());
    }

    private void resumeSong() {
        musicService.resumeSong();
        setPauseIcon();
    }

    private void pauseSong() {
        if (musicService == null)
            return;

        musicService.pauseSong();
        setPlayIcon();
    }

    private void playPreviousSong() {
        if (songs.isEmpty() || musicService == null)
            return;

        musicService.playPreviousSong();
        setPauseIcon();
        updateSongInfo(musicService.getCurrSong());
    }

    private void playNextSong() {
        if (songs.isEmpty() || musicService == null)
            return;

        musicService.playNextSong();
        updateSongInfo(musicService.getCurrSong());
        setPauseIcon();
    }

    private void setPlayIcon() {
        playPauseBtn.setImageDrawable(getResources().getDrawable(R.mipmap.play));
    }

    private void setPauseIcon() {
        playPauseBtn.setImageDrawable(getResources().getDrawable(R.mipmap.pause));
    }

    private boolean isPlaylistEmpty() {
        if (songs == null || songs.isEmpty()) {
            Utils.showToast(this, R.string.playlist_empty);
            return true;
        }
        return false;
    }
}