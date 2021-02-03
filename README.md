[![](https://travis-ci.com/BIOP/EasyXT-FIJI.svg?branch=master)](https://travis-ci.com/BIOP/EasyXT-FIJI)

# EasyXT for ImageJ

![EasyXT Image](https://raw.githubusercontent.com/lacan/EasyXT/master/EasyXT-Logo.jpg)

![ScreenShot](https://raw.githubusercontent.com/BIOP/EasyXT-FIJI/master/images/screenshot.png)

An Imaris Xtension for ImageJ. 

This collection of Java-friendly APIs can help simplify the interaction between ImageJ and Imaris when writing XTensions.

## Installation for End Users

### In Fiji
To install EasyXT, you need to add and activate the following update site in Fiji
`https://biop.epfl.ch/Fiji-EasyXT/`

### In Imaris
1. Under `File > Preferences > Custom Tools` make sure to set the Fiji folder
2. Open an image
3. Use `Fiji>Image To Fiji` which will make Imaris copy the necessary propietary JARs into your Fiji Installation

### To Test
1. Open a small dataset in Imaris
2. Open Fiji
3. Go to `Plugins>BIOP>EasyXT>Get Complete Imaris Dataset`
4. You should have our dataset in Fiji Now

## For Developers
Requirements : Imaris version above 9.5.1 

To work on the repository : 
1. Have Imaris installed 
2. Clone the repository
4. Run Imaris
5. Ready to go ! - You can run the EasyXT Main method

## JavaDoc
There is an as-complete-as-possible JavaDoc available at
https://biop.github.io/EasyXT-FIJI/javadoc/apidocs/

## Examples

You will find example code in the `ch/epfl/biop/imaris/demo` folder

## Roadmap
Currently, we are working on getting the API to work and stabilize, before creating ImageJ Commands that can be macro-recorded. This project is mainly intended to be used as an API when writing Groovy scripts. 