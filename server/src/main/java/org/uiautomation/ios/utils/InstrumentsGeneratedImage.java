/*
 * Copyright 2012-2013 eBay Software Foundation and ios-driver committers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.uiautomation.ios.utils;

import org.apache.commons.codec.binary.Base64;
import org.openqa.selenium.WebDriverException;
import org.uiautomation.ios.UIAModels.Orientation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


/**
 * takes care of reading the screenshots generated by instruments, rotate them to what they are
 * supposed to be and serialize them for the JSONWire protocol.
 */
public class InstrumentsGeneratedImage implements JSONWireImage {

  private final File source;
  private final Orientation orientation;
  private final BufferedImage image;


  public InstrumentsGeneratedImage(File source, Orientation orientation) {
    this.source = source;
    this.orientation = orientation;
    try {
      waitForFileToAppearOnDisk();
      image = rotate();
    } catch (InterruptedException e) {
      throw new WebDriverException(
          "Interrupted waiting for the screenshot to be written on disk.", e);
    } finally {
      source.delete();
    }
  }

  @Override
  public String getAsBase64String() {

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(image, "PNG", out);
      byte[] img = out.toByteArray();
      String s = Base64.encodeBase64String(img);
      return s;
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      return null;
    }
  }

  private BufferedImage rotate() {
    int rotateDegrees = orientation.getRotationInDegree();
    boolean
        flip =
        orientation == Orientation.LANDSCAPE
        || orientation == Orientation.UIA_DEVICE_ORIENTATION_LANDSCAPERIGHT;
    try {
      final BufferedImage originalImage = ImageIO.read(source);

      // no rotation needed.
      if (rotateDegrees == 0) {
        return originalImage;
      }

      // need to rotate.
      final BufferedImage rotated;
      int width;
      int height;
      if (flip) {
        width = originalImage.getHeight();
        height = originalImage.getWidth();
      } else {
        width = originalImage.getWidth();
        height = originalImage.getHeight();
      }
      rotated = new BufferedImage(width, height, originalImage.getType());

      // Rotate the image and then move it back up to the origin through a translation call, since it'll pivot around
      // the center point which will cause non-square images to offset by the different in height and width.
      final Graphics2D graphics = rotated.createGraphics();
      graphics.rotate(Math.toRadians(rotateDegrees), rotated.getWidth() / 2,
                      rotated.getHeight() / 2);
      graphics.translate((rotated.getWidth() - originalImage.getWidth()) / 2,
                         (rotated.getHeight() - originalImage.getHeight()) / 2);
      graphics
          .drawImage(originalImage, 0, 0, originalImage.getWidth(), originalImage.getHeight(),
                     null);
      return rotated;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void waitForFileToAppearOnDisk() throws InterruptedException {

    int cpt = 0;
    while (!source.exists()) {
      Thread.sleep(250);
      cpt++;
      if (cpt > 5 * 4) {
        throw new WebDriverException("timeout waiting for screenshot file to be written.");
      }
    }
    return;
  }
}
