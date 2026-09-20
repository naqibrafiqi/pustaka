### **Structure**

1. Set a folder, Pustaka, in the phone storage (`/storage/emulated/0/Pustaka`)
2. Below are the folder tree:
```
   - ../Pustaka
     - 01a0b844-afbc-75b6-969a-f446fedefb8a
       - vol\_01.epub
       - vol\_02.epub
       - vol\_03.epub
         - ..
     - 01a0b844-afbc-75b6-969a-fb874bb07a8c	    
       - vol\_01.epub
       - vol\_02.epub
       - ..
     - 01a0b844-afbc-75b6-969a-fd75f21a83ae
       - vol\_01.epub
       - ..
     - ..
```

### **Implementation**

* The folder by default set `/storage/emulated/0/Pustaka` as the root folder, but user can change the location for the app to scan the books.
* All the book will be added to a series, never individually. It means that user needs to create a series first, then add book(s) from there.
* When adding series in the app, it will automatically create folder in the Pustaka folder with the of its own unique UUID (refer to **Structure** section). For now, user are advised to **NOT** add or create the series folder inside Pustaka folder because the app will not recognize it.
* For the books, the app will assume that the name will be as this, _`..., Vol. 04.epub`_, so it will extract the last two characters (_04_) that will be the volume number to be appended with _`vol\_`_ becoming _`vol\_04.epub`_.
* If the books don't have that name format, it will increment the last known volume or set it as _1_ if there's no known volume.
* When selecting the books to be moved into the series folder, the app will show the list of the books to moved, and it's new name. User can check and update the name.

