// Landmarks.java
package net.smallacademy.cameraandgallery;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Landmarks {
    private static final String TAG = "Landmarks";

    public void processImage(Bitmap bitmap) {
        // Process the Bitmap image data here
        // ...
    }

    // Jim Code
    private List<Double> average_ROI(Mat input_image, List<Point> points_of_interest) {
        //    Mat image = /* Your input image */;
//    List<Point> hullPoints = /* The points of your convex hull */;
        MatOfPoint hull = new MatOfPoint();
        hull.fromList(points_of_interest);

// Step 1: Create the convex hull
        MatOfInt hullInt = new MatOfInt();
        Imgproc.convexHull(hull, hullInt);

        //Intermediate step convert Indexes to points

        MatOfPoint mopOut = new MatOfPoint();
        mopOut.create((int)hullInt.size().height,1,CvType.CV_32SC2);

        for(int i = 0; i < hullInt.size().height ; i++)
        {
            int index = (int)hullInt.get(i, 0)[0];
            double[] point = new double[] {
                    hull.get(index, 0)[0], hull.get(index, 0)[1]
            };
            mopOut.put(i, 0, point);
        }

// Step 2: Create the binary mask
        Mat mask = Mat.zeros(input_image.size(), CvType.CV_8UC1);
//    Mat temporary_mat = new Mat();
        Imgproc.fillConvexPoly(mask, mopOut,Scalar.all(255));

//// Step 3: Calculate the average color of the pixels inside the convex hull
        Scalar mean = Core.mean(input_image, mask);

// The average color values are stored in the mean variable
        double averageRed = mean.val[0];
        double averageGreen = mean.val[1];
        double averageBlue = mean.val[2];

        List<Double> rgb_avg = new ArrayList<>();
        rgb_avg.add(averageRed);
        rgb_avg.add(averageGreen);
        rgb_avg.add(averageBlue);

        mask.release();
//    input_image.release();

        return rgb_avg;
    }

    private List<Point> slice_list(List<Point> points, int[] indicesToSlice) {
        List<Point> slicedPoints = new ArrayList<Point>();
        for (int i : indicesToSlice) {
            slicedPoints.add(points.get(i));
        }
        return slicedPoints;
    }

    private void logNoseLandmark(FaceMeshResult result, boolean showPixelValues) {
        if (result == null || result.multiFaceLandmarks().isEmpty()) {
            return;
        }

        float timestamp = result.timestamp();
        double timestamp_sec = timestamp/1e6;

        Mat matdummyY = new Mat();
        Mat big_img = new Mat();
        Mat matdummy = new Mat();

        Utils.bitmapToMat(result.inputBitmap(), big_img);
        Imgproc.resize(big_img, matdummyY, new Size(0, 0), 0.25, 0.25,
                Imgproc.INTER_AREA);
        Imgproc.cvtColor(matdummyY, matdummy, Imgproc.COLOR_RGBA2RGB);
        big_img.release();
        matdummyY.release();


        int width1 = matdummy.width();
        int height1 = matdummy.height();
        Core.flip(matdummy, matdummy, 0); //image is upside down and mirrored so need to flip

        List<Point> landmarks = new ArrayList<>();
        for (NormalizedLandmark landmark : result.multiFaceLandmarks().get(0).getLandmarkList()) {
            int x = (int)Math.round(landmark.getX() * width1);
            int y = (int)Math.round(landmark.getY() * height1);
            landmarks.add(new Point(x, y));
        }
//    full face
        List<Double> rgb_avg = average_ROI(matdummy, landmarks);

//    forehead
        int[] left_fh_t = {54, 68, 104, 69, 67, 103};
        List<Point> left_fh_t_p = slice_list(landmarks, left_fh_t);
        List<Double> left_fh_t_avg = average_ROI(matdummy, left_fh_t_p);

        int[] left_fh_b = {68, 63, 105, 66, 69, 104};
        List<Point> left_fh_b_p = slice_list(landmarks, left_fh_b);
        List<Double> left_fh_b_p_avg = average_ROI(matdummy, left_fh_b_p);

        int[] center_fh_lt = {67, 69, 108, 151, 10, 109};
        List<Point> center_fh_lt_p = slice_list(landmarks, center_fh_lt);
        List<Double> center_fh_lt_avg = average_ROI(matdummy, center_fh_lt_p);

        int[] center_fh_lb = {69, 66, 107, 9, 151, 108};
        List<Point> center_fh_lb_p = slice_list(landmarks, center_fh_lb);
        List<Double> center_fh_lb_avg = average_ROI(matdummy, center_fh_lb_p);

        int[] center_fh_rt = {10, 151, 337, 299, 297, 338};
        List<Point> center_fh_rt_p = slice_list(landmarks, center_fh_rt);
        List<Double> center_fh_rt_p_avg = average_ROI(matdummy, center_fh_rt_p);

        int[] center_fh_rb = {151, 9, 336, 296, 299, 337};
        List<Point> center_fh_rb_p = slice_list(landmarks, center_fh_rb);
        List<Double> center_fh_rb_avg = average_ROI(matdummy, center_fh_rb_p);

        int[] right_fh_t = {297, 299, 333, 298, 284, 332};
        List<Point> right_fh_t_p = slice_list(landmarks, right_fh_t);
        List<Double> right_fh_t_avg = average_ROI(matdummy, right_fh_t_p);

        int[] right_fh_b = {299, 296, 334, 293, 298, 333};
        List<Point> right_fh_b_p = slice_list(landmarks, right_fh_b);
        List<Double> right_fh_b_avg = average_ROI(matdummy, right_fh_b_p);

        int[] center_fh_b = {107, 55, 193, 168, 417, 285, 336, 9};
        List<Point> center_fh_b_p = slice_list(landmarks, center_fh_b);
        List<Double> center_fh_b_p_avg = average_ROI(matdummy, center_fh_b_p);

//    nose
        int[] nose_top = {193, 122, 196, 197, 419, 351, 417, 168};
        List<Point> nose_top_p = slice_list(landmarks, nose_top);
        List<Double> nose_top_p_avg = average_ROI(matdummy, nose_top_p);

        int[] nose_bot = {196, 3, 51, 45, 275, 281, 248, 419, 197};
        List<Point> nose_bot_p = slice_list(landmarks, nose_bot);
        List<Double> nose_bot_p_avg = average_ROI(matdummy, nose_bot_p);

//    left cheek
        int[] lc_t = {31, 117, 50, 101, 100, 47, 114, 121, 230, 229};
        List<Point> lc_t_p = slice_list(landmarks, lc_t);
        List<Double> lc_t_p_avg = average_ROI(matdummy, lc_t_p);

        int[] lc_b = {50, 187, 207, 206, 203, 129, 142, 101};
        List<Point> lc_b_p = slice_list(landmarks, lc_b);
        List<Double> lc_b_p_avg = average_ROI(matdummy, lc_b_p);


//    right cheek
        int[] rc_t = {261, 346, 280, 330, 329, 277, 343, 350, 450, 449, 448};
        List<Point> rc_t_p = slice_list(landmarks, rc_t);
        List<Double> rc_t_p_avg = average_ROI(matdummy, rc_t_p);

        int[] rc_b = {280, 411, 427, 426, 423, 358, 371, 330};
        List<Point> rc_b_p = slice_list(landmarks, rc_b);
        List<Double> rc_b_p_avg = average_ROI(matdummy, rc_b_p);

        matdummy.release();
        Log.i(
                TAG,
                String.format(
                        "Time=%f, Blue Average=%f, Green Average=%f, Red Average=%f",
                        timestamp_sec,rgb_avg.get(2), rgb_avg.get(1), rgb_avg.get(0)));

    }
}
