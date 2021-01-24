import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;
import org.opencv.core.*;
import org.opencv.highgui.HighGui.*;
import org.opencv.imgcodecs.Imgcodecs;
import javax.imageio.ImageIO;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;

import org.opencv.videoio.VideoCapture;
import java.awt.image.DataBufferInt;

public class UI {
    JFrame videoFrame;
    ImagePanel videoPanel;

    VideoCapture videoCam;

    Mat frame;
    MatOfByte memory;

    Runnable videoRunnable;
    Thread videoThread;
    VisionPipeline visionPipeline;

    public UI() {
        videoFrame = new JFrame("Camera Feed");
        videoPanel = new ImagePanel();
        visionPipeline = new VisionPipeline();
        videoFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        videoCam = new VideoCapture(0);
        frame = new Mat();
        memory = new MatOfByte();

        videoFrame.add(videoPanel);

        videoFrame.pack();
        videoFrame.setVisible(true);

        videoRunnable = new Runnable() {
            public volatile boolean runnable = true;

            public Mat buffToMat(BufferedImage sourceImg) {

                DataBuffer dataBuffer = sourceImg.getRaster().getDataBuffer();
                byte[] imgPixels = null;
                Mat imgMat = null;

                int width = sourceImg.getWidth();
                int height = sourceImg.getHeight();

                if (dataBuffer instanceof DataBufferByte) {
                    imgPixels = ((DataBufferByte) dataBuffer).getData();
                }

                if (dataBuffer instanceof DataBufferInt) {

                    int byteSize = width * height;
                    imgPixels = new byte[byteSize * 3];

                    int[] imgIntegerPixels = ((DataBufferInt) dataBuffer).getData();

                    for (int p = 0; p < byteSize; p++) {
                        imgPixels[p * 3 + 0] = (byte) ((imgIntegerPixels[p] & 0x00FF0000) >> 16);
                        imgPixels[p * 3 + 1] = (byte) ((imgIntegerPixels[p] & 0x0000FF00) >> 8);
                        imgPixels[p * 3 + 2] = (byte) (imgIntegerPixels[p] & 0x000000FF);
                    }
                }

                if (imgPixels != null) {
                    imgMat = new Mat(height, width, CvType.CV_8UC3);
                    imgMat.put(0, 0, imgPixels);
                }
                return imgMat;
            }

            public BufferedImage matToBuff(Mat mat) throws IOException
           {
                MatOfByte matMemory = new MatOfByte();

                Imgcodecs.imencode(".bmp", mat, matMemory);
                Image img =  ImageIO.read(new ByteArrayInputStream(matMemory.toArray()));
                               
                return (BufferedImage) img;
           }

           @Override
           public void run() 
           {
               synchronized(this)
               {
                   while(runnable)
                   {
                       if(videoCam.grab())
                       {
                           try
                           {
                               videoCam.retrieve(frame);
                               Imgcodecs.imencode(".bmp", frame, memory);
                               Image im =  ImageIO.read(new ByteArrayInputStream(memory.toArray()));
                               
                               BufferedImage buff = (BufferedImage) im;
                               
                               //convert the Bufferedimage into a Mat object
                               Mat imgToMat = buffToMat(buff);
                               
                               //Plugging in the image into the pipeline
                               visionPipeline.process(imgToMat);
                               Mat pipelineMat = visionPipeline.maskOutput();

                               //Mat to Buffered Image                              
                               BufferedImage pipelineBuffImg = matToBuff(pipelineMat);
                               
                               videoPanel.updateImage(pipelineBuffImg);

        
                               if(runnable == false)
                               {
                                   System.out.println("Waiting");
                                   this.wait();
                               }

                           }
                           catch(Exception ex)
                           {
                                System.out.println(ex);
                                System.out.println("Something Happend");
                           }
                       }
                   }
               }
           }

       };

       videoThread = new Thread(videoRunnable);
       videoThread.setDaemon(true);
       videoThread.start();
   }
    
}
