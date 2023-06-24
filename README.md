# SeamCarver
An application that utilizes content-aware image resizing.

![image](https://github.com/StormyTalents/SeamCarver/assets/98739389/b982ded5-d5e4-4d5e-bf2d-980546a09c00)

<h3>Using a technique called <a href="http://graphics.cs.cmu.edu/courses/15-463/2007_fall/hw/proj2/imret.pdf">seam-carving</a>, this application allows you to take an image and resize it without noticeably effecting the content.</h3>

<p>Stretching, shrinking or cropping makes it obvious that an image has been modified, but seam-carving works by removing or adding pixels in the least noticeable locations in order to reduce or enlarge the size of the image. This application provides a UI for my implementation of the seam-carving algorithm. Above is an example of reducing the size of an image using content-aware resizing (the removed pixels are in red).</p>
