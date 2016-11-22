# CMSC434-Impressionist

References:  
Saving images 
http://stackoverflow.com/questions/14053338/save-bitmap-in-android-as-jpeg-in-external-storage-in-a-folder

Functions:  
Load image from gallery by hitting "Load Image" and then selecting an image to load.  
Select brush by tapping "Brush" (All brushes changes size based on speed of movement):
  * Circle brush - paints a circle of the color based on the corresponding location in the image
  * Square brush - paints a square of the color based on the corresponding location in the image
  * Line brush - paints a line slanted away from the direction of movement with color based on the corresponding location in the image  

Then move your finger inside the black rectangle on the right side the draw.  
"Clear" clears the right-hand side canvas, but leaves image on left alone.
To save, it "Save Image" which automatically saves the image to the SD card in a folder called "Impressionist".  
"Auto" automatically generates 1000 points with random size brush with the selected brush. Can be tapped repeatedly to fill out image.

Tower Bridge in London, UK
![example](https://github.com/rsharan2/CMSC434-Impressionist/blob/master/Impressionist_12.PNG)
