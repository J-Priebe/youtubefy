package priebe.google.youtube;

/**
 * Simple tool for creating youtube playlists from text files. Some of the code
 * framework is based on youtube api sample from google and the Oracle
 * JFileChooser Documentation.
 *
 * @author James
 */
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.File;
import java.io.IOException;
import java.util.List;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Youtubefy extends JPanel {

    JButton openButton, goButton;
    JLabel playlistPrompt, fileNameLabel, filePrompt;
    JComboBox playlistsDropDown;
    JTextField playlistTextBox;
    JFileChooser fc;

    File playlistFile = null;

    List<String> playlistIds; //preexisting playlist of user's account

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final java.io.File DATA_STORE_DIR
            = new java.io.File(System.getProperty("user.home"), ".store/youtube_sample");
    private static FileDataStoreFactory dataStoreFactory;

    private static YouTube youtube;

    private String[] openFile(File f) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));

            List<String> arr = new ArrayList();
            String line = br.readLine();

            while (line != null) {
                arr.add(line);
                line = br.readLine();
            }
            br.close();

            String[] lines = new String[arr.size()];
            for (int i = 0; i < lines.length; i++) {
                lines[i] = arr.get(i);
            }
            return lines;
        } catch (IOException e) {
            return null;
        } 
    }

    // gets playlist ids for later use as well as returning their titles
    // so they can be displayed in a combo box.
    private String[] getExistingPlaylists() throws IOException {

        YouTube.Playlists.List request = youtube.playlists().list("snippet");
        request.setMine(true);

        PlaylistListResponse result = request.execute();

        List<Playlist> playlists = result.getItems();

        String[] titles = new String[playlists.size() + 1];
        titles[0] = "New Playlist";

        for (int i = 1; i < titles.length; i++) {
            titles[i] = playlists.get(i - 1).getSnippet().getTitle();

        }

        playlistIds = new ArrayList();
        for (int i = 0; i < playlists.size(); i++) {
            playlistIds.add(playlists.get(i).getId());

        }
        return titles;
    }

    // returns playlist id
    private String createNewPlaylist(String name, boolean isPrivate) {

        PlaylistSnippet playlistSnippet = new PlaylistSnippet();
        playlistSnippet.setTitle(name);
        playlistSnippet.setDescription("Created with Youtubefy. https://github.com/J-Priebe/youtubefy");

        PlaylistStatus playlistStatus = new PlaylistStatus();
        if (isPrivate) {
            playlistStatus.setPrivacyStatus("private");
        } else {
            playlistStatus.setPrivacyStatus("public");
        }

        Playlist youTubePlaylist = new Playlist();
        youTubePlaylist.setSnippet(playlistSnippet);
        youTubePlaylist.setStatus(playlistStatus);

        YouTube.Playlists.Insert playlistInsertCommand;
        try {
            playlistInsertCommand = youtube.playlists().insert("snippet,status", youTubePlaylist);
            Playlist playlistInserted = playlistInsertCommand.execute();
            return playlistInserted.getId();

        } catch (IOException ex) {
            return null;
        }

    }

    // get video id from query. return null if no results
    private String searchYoutube(String query) {

        try {
            YouTube.Search.List search = youtube.search().list("id,snippet");
            search.setQ(query);
            search.setType("video");
            search.setFields("items(id/videoId)"); //returns only info we care about
            search.setMaxResults(1L); //only care about top result
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            if (searchResultList != null) {

                SearchResult result = searchResultList.get(0);
                ResourceId rId = result.getId();
                String vId = rId.getVideoId();
                return vId;
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private void addVideo(String videoId, String playlistId) {

        ResourceId resourceId = new ResourceId();
        resourceId.setKind("youtube#video");
        resourceId.setVideoId(videoId);

        PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
        playlistItemSnippet.setPlaylistId(playlistId);
        playlistItemSnippet.setResourceId(resourceId);

        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setSnippet(playlistItemSnippet);

        try {
            YouTube.PlaylistItems.Insert playlistItemsInsertCommand
                    = youtube.playlistItems().insert("snippet,contentDetails", playlistItem);
            PlaylistItem returnedPlaylistItem = playlistItemsInsertCommand.execute();
        } catch (IOException e) {

        }
        //return returnedPlaylistItem.getId();
    }

    public Youtubefy() throws IOException {

        //Create a file chooser restricted to .txt files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt");
        fc = new JFileChooser();
        fc.setFileFilter(filter);

        // file selection
        filePrompt = new JLabel("Open the text file containing your playlist.");
        fileNameLabel = new JLabel("No file selected.");
        openButton = new JButton("Browse Files");
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fc.showOpenDialog(Youtubefy.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {

                    playlistFile = fc.getSelectedFile();
                    if (playlistFile.getName().endsWith(".txt")) {
                        fileNameLabel.setText(playlistFile.getName());
                    } else {
                        playlistFile = null;
                        fileNameLabel.setText("Oops! File must be .txt");

                    }
                }

            }
        });
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.add(filePrompt, BorderLayout.PAGE_START);
        filePanel.add(openButton);
        filePanel.add(fileNameLabel, BorderLayout.PAGE_END);

        // playlist selection
        playlistPrompt = new JLabel("Choose a YouTube playlist to add to.");
        String[] s = getExistingPlaylists();
        playlistsDropDown = new JComboBox(s);
        playlistsDropDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (playlistsDropDown.getSelectedIndex() == 0) {
                    playlistTextBox.setEditable(true);
                    playlistTextBox.setText("New Playlist");
                } else {
                    playlistTextBox.setEditable(false);
                    playlistTextBox.setText(playlistsDropDown.getSelectedItem().toString());
                }
            }
        });
        playlistTextBox = new JTextField("New Playlist");
        JPanel playlistPanel = new JPanel(new BorderLayout(5, 5));
        playlistPanel.add(playlistPrompt, BorderLayout.PAGE_START);
        playlistPanel.add(playlistsDropDown);
        playlistPanel.add(playlistTextBox, BorderLayout.PAGE_END);

        goButton = new JButton("Go!");
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                String pid;
                if (playlistFile != null) {

                    // new playlist
                    if (playlistsDropDown.getSelectedIndex() == 0) {
                         pid = createNewPlaylist(playlistTextBox.getText(), true);
                    } else { //existing playlist     
                         pid = playlistIds.get(playlistsDropDown.getSelectedIndex() - 1);
                    }

                    //where the magic happens
                    String[] queries = openFile(playlistFile);
                    for (String query : queries) {
                        String vid = searchYoutube(query);
                        addVideo(vid, pid);
                    }

                    System.out.println("Playlist created!");

                } else {
                    System.out.println("No file selected.");
                }

            }
        });

        add(filePanel);
        add(playlistPanel);
        add(goButton);

    }

    private static void createAndShowGUI() throws IOException {

        JFrame frame = new JFrame("Youtubefy");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new Youtubefy());
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }

    private static Credential authorize(List<String> scopes) throws IOException {

        // Load client secrets.
        final String path = "resources/client_secrets.json";
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY, new InputStreamReader(Youtubefy.class.getResourceAsStream(path)));

        // Checks that the defaults have been replaced (Default = "Enter X here").
        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            System.out.println(
                    "Enter Client ID and Secret from https://code.google.com/apis/console/?api=youtube"
                    + "into youtube-cmdline-addfeaturedvideo-sample/src/main/resources/client_secrets.json");
            System.exit(1);
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes)
                .setDataStoreFactory(dataStoreFactory)
                .build();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    private static void startYoutubeService() throws IOException {

        dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

        List<String> scopes = new ArrayList();
        scopes.add(YouTubeScopes.YOUTUBE);
        scopes.add(YouTubeScopes.YOUTUBE_READONLY);
        scopes.add(YouTubeScopes.YOUTUBE_UPLOAD);
        scopes.add(YouTubeScopes.YOUTUBEPARTNER);
        scopes.add(YouTubeScopes.YOUTUBEPARTNER_CHANNEL_AUDIT);
        // authorization
        Credential credential;
        credential = authorize(scopes);

        // set up global YouTube instance
        youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("Youtubefy").build();

    }

    public static void main(String[] args) {

        try {
            startYoutubeService();

        } catch (IOException ex) {
            System.out.println("Something went wrong");
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    createAndShowGUI();
                } catch (IOException ex) {
                    Logger.getLogger(Youtubefy.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

    }
}
