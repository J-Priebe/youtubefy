#### Youtubefy
#### Create a Youtube playlist from a text file
#### By James Priebe
#### jamespriebe92@gmail.com


import httplib2
import os
import sys
from apiclient.discovery import build
from oauth2client.file import Storage
from oauth2client.client import flow_from_clientsecrets
from oauth2client.tools import run


####### IMPORTANT FOR SOME REASON ##################
import gflags

FLAGS = gflags.FLAGS

def main(argv):
  try:
    argv = FLAGS(argv)  # parse flags
  except gflags.FlagsError, e:
    print '%s\\nUsage: %s ARGS\\n%s' % (e, sys.argv[0], FLAGS)
    sys.exit(1)
####################################################################


CLIENT_SECRETS_FILE = "C:\Users\James\Dropbox\client_secrets.json"  #TBI: abs path
YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"

# Opens web browser for user to grant app permissions
def get_authenticated_service():

    flow = flow_from_clientsecrets(CLIENT_SECRETS_FILE, scope=YOUTUBE_SCOPE,
        message="missing client secrets json file")

    storage = Storage('credentials.dat')
    credentials = storage.get()
    if credentials is None or credentials.invalid:
        credentials = run(flow, storage)

    http = credentials.authorize(httplib2.Http())
    return build('youtube','v3', http=http)

def get_filename():
    print 'Enter the name of your file that contains song information'
    print 'e.g., myplaylist.txt'
    # TBI: get local path of filename with system API
    return raw_input()

# prompt user to name their new playlist
def get_playlist_name():
    print 'Enter the name of your playlist.'
    name = raw_input()
    return name
  
# returns array of lines to be used as search queries
def get_search_list(filename): 
    
    try:
        with open(filename, 'r') as f:
            lines = f.readlines()
    except (IOError):
        print 'Error loading text file.'

    return lines


def find_video(searchterms):

    search_response = youtube.search().list(
        q=searchterms,  # the search query
        part="snippet",  #search by title, description, etc; not videoID
        safeSearch="none", #do not filter results
        type="video",  #do not include playlists or channels
    ).execute()

    #returns video ID of the best match
    search_result = search_response.get("items")[0]
    return search_result["id"]

# Creates the new, empty playlist and return its ID 
def create_playlist(name): 
    new_playlist = youtube.playlists().insert(
        part="snippet",
        body=dict(
            snippet=dict(
                title=name,
                description="Playlist created by Youtubefy."
            )
        )
    ).execute()

    pid = new_playlist.get("id")
    return pid

# adds video to playlist. IDs are found with find_video and create_playlist  
def add_video(videoID, playlistID):
    youtube.playlistItems().insert(
        part="snippet",
        body=dict(
            snippet=dict(
                playlistId = playlistID,
                resourceId = videoID
            )
        )
    ).execute()


### MAIN ###

# the youtube service that recieves api calls
youtube = get_authenticated_service()

playlist_name = get_playlist_name()
playlist_id = create_playlist(playlist_name)
filename = get_filename()

# list of search queries
searches = get_search_list(filename)

for search_query in searches:
  video_id = find_video(search_query)
  add_video(video_id,playlist_id)    
