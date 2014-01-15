youtubefy
generate youtube playlists from text file of song information
=========

Originally written in python, I gave up as Google seemed to be deprecating their APIs faster than I could use them.
Eventually ended up in an unworkable state, so it was remade in Java with Youtube API v3 and OAuth 2

To run from source you will need to include the relevant API depencies, found here:
https://developers.google.com/api-client-library/java/apis/youtube/v3


How to use:
All you need is a text file containing information about the songs you want, line by line. For example:

Artist - Song

Song2 - Artist2

Artist

Misspelled song

run the .jar, authenticate your account, and a youtube playlist will automatically be created.
Since it's just executing a series of youtube searches, the format and spelling are not terribly important.
Enjoy!


James Priebe
jamespriebe92@gmail.com
