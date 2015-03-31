package org.opencv.samples.tutorial2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.math.*;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import com.googlecode.tesseract.android.TessBaseAPI;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class Tutorial2Activity extends Activity implements CvCameraViewListener2 {
    private static final String    TAG = "OCVSample::Activity";

    private static final int       VIEW_MODE_Step1     = 0;
    private static final int       VIEW_MODE_Step2     = 1;
    private static final int       VIEW_MODE_Step3     = 2;
    private static final int       VIEW_MODE_Step4     = 3;
    private static final int       VIEW_MODE_Step5     = 4;
    private String 				   tofindText          = "call";
    private Mat					   tofindImg           = null;
    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat                    mGray;

    private MenuItem               mItemPreviewStep1;
    private MenuItem               mItemPreviewStep2;
    private MenuItem               mItemPreviewStep3;
    private MenuItem               mItemPreviewStep4;
    private MenuItem               mItemPreviewStep5;

    private CameraBridgeViewBase   mOpenCvCameraView;
    //OCR VARS
	public static final String lang = "eng";
	protected String _path;
	protected boolean _taken;
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/WordFinder/";
	TessBaseAPI baseApi = null;
	private static double PAD = 0.4;
	private static double ANGLE_THRESH=5;
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("mixed_sample");

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public Tutorial2Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.tutorial2_surface_view);
        AlertDialog.Builder dialogBuilder;
        dialogBuilder=new AlertDialog.Builder(this);
        final EditText txtinput=new EditText(this);
        dialogBuilder.setTitle("Word To Find");
        dialogBuilder.setMessage("Enter the Word");
        dialogBuilder.setView(txtinput);
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				tofindText=txtinput.getText().toString();
            	tofindImg=mainTextInitializr(tofindText);

			}
		}) ;
			AlertDialog DialoginputWord=dialogBuilder.create();
			DialoginputWord.show();
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial2_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mViewMode = VIEW_MODE_Step4;
        
        
        //OCR ON CREATE
        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

		for (String path : paths) {
			File dir = new File(path);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
					return;
				} else {
					Log.v(TAG, "Created directory " + path + " on sdcard");
				}
			}

		}
		if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
			try {

				AssetManager assetManager = getAssets();
				InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
				//GZIPInputStream gin = new GZIPInputStream(in);
				OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/" + lang + ".traineddata");
				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				//while ((lenf = gin.read(buff)) > 0) {
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				//gin.close();
				out.close();
				Log.v(TAG, "Copied " + lang + " traineddata");
			} catch (IOException e) {
				Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
			}
		}
		_path = DATA_PATH + "/ocr.jpg";
		baseApi = new TessBaseAPI();
		baseApi.setDebug(true);
		baseApi.init(DATA_PATH, lang);  
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewStep1= menu.add("Step1");
        mItemPreviewStep2 = menu.add("Step2");
        mItemPreviewStep3 = menu.add("Step3");
        mItemPreviewStep4 = menu.add("Step4");
        mItemPreviewStep5 = menu.add("Step5");
        return true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);  
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	
    	
        final int viewMode = mViewMode;
        switch (viewMode) {
        case VIEW_MODE_Step1:
            // input frame has gray scale format
            //Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            break;
        case VIEW_MODE_Step2:
            // input frame has RBGA format
            mRgba = inputFrame.rgba();
            break;
        case VIEW_MODE_Step3:
            // input frame has gray scale format
            mRgba = inputFrame.rgba();
            Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80, 100);
            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            break;
        case VIEW_MODE_Step4:
        	//BEGINING OF NEW CODE OF DETECTION
            mRgba = inputFrame.rgba();
            Mat bwImage,filteredImage;
            filteredImage=new Mat();
            Imgproc.medianBlur(inputFrame.gray(), filteredImage, 3);
            
            //gray2bw
            bwImage=new Mat();
            Imgproc.adaptiveThreshold(filteredImage, bwImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 51, 10);
            double angle= calcAngle(bwImage);
            
            
            // input frame has gray scale format
            mRgba = inputFrame.rgba();
            int maxValue = 255;
            int blockSize = 61;
            int meanOffset = 20;
            Imgproc.adaptiveThreshold(inputFrame.gray(), mIntermediateMat, maxValue, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, blockSize, meanOffset);
            Mat kernel=Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(9,2));
            if(tofindImg==null)
            {
            	tofindImg=mainTextInitializr(tofindText);

            }
            Mat closed=new Mat();
            Imgproc.morphologyEx(mIntermediateMat, closed, Imgproc.MORPH_CLOSE, kernel);
            //addtoframe(kernel, 50, 50);
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(closed,contours,new Mat(),Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE); 

            for(int i=0;i<contours.size();i++)
            {
            	MatOfPoint Temp=new MatOfPoint(contours.get(i));
            	org.opencv.core.Rect rect= Imgproc.boundingRect(Temp);
            	//if(rect.height>=tofindImg.height() && rect.width>tofindImg.width())
            	{
            		Mat TestCase=null;
            		try{
            		Rect NewRect=new Rect();
            		NewRect.y=(int) (rect.y-rect.height*0.20);
            		NewRect.x=rect.x;
            		NewRect.height=(int)(rect.height+rect.height*0.40);
            		NewRect.width=rect.width;
            		TestCase=new Mat(mIntermediateMat,NewRect);
            		rect=NewRect;
            		}
            		catch(Exception e)
            		{
                		TestCase=new Mat(mIntermediateMat,rect);

            		}
            		DoBackGroundCalc MyThread=new DoBackGroundCalc();

            		try {
            			MyThread.TestCaseTemp=TestCase;
            			String result=MyThread.execute().get();
            			if(tofindText.length()!=0 && result.length()!=0){
            			if(getLevenshteinDistance(tofindText, result)==0)
                    		Core.rectangle(mRgba, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(0,255,0,255),2);
                    		//if(result.endsWith(tofindText))
                    		else if(getLevenshteinDistance(tofindText, result)<2)
                    		Core.rectangle(mRgba, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(0,0,255,255),2);
                    		else if(getLevenshteinDistance(tofindText, result)<3)
                    		Core.rectangle(mRgba, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(255,0,0,255),2);
            			}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	
            	}
            }
            if(Math.abs(angle)>ANGLE_THRESH)
            	Core.putText(mRgba,"theta="+String.valueOf((int)angle+" please adjust the mobile with the text"), new Point(10,50),3 , 1, new Scalar(255,0,0,255),2);
            else
            	Core.putText(mRgba,"theta="+String.valueOf((int)angle+" is OK"), new Point(10,50),3 , 1, new Scalar(0,255,0,255),2);

            addtoframe(tofindImg, 70, 70);
        
            break;
        case VIEW_MODE_Step5:
            // input frame has RGBA format
            mRgba = inputFrame.rgba();
            mGray = inputFrame.gray();
            FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
            break;

        
    	}
        return mRgba;
    	
    }
    public static int getLevenshteinDistance(String s, String t) {
        if (s == null || t == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }

        /*
           The difference between this impl. and the previous is that, rather 
           than creating and retaining a matrix of size s.length()+1 by t.length()+1, 
           we maintain two single-dimensional arrays of length s.length()+1.  The first, d,
           is the 'current working' distance array that maintains the newest distance cost
           counts as we iterate through the characters of String s.  Each time we increment
           the index of String t we are comparing, d is copied to p, the second int[].  Doing so
           allows us to retain the previous cost counts as required by the algorithm (taking 
           the minimum of the cost count to the left, up one, and diagonally up and to the left
           of the current cost count being calculated).  (Note that the arrays aren't really 
           copied anymore, just switched...this is clearly much better than cloning an array 
           or doing a System.arraycopy() each time  through the outer loop.)

           Effectively, the difference between the two implementations is this one does not 
           cause an out of memory condition when calculating the LD over two very large strings.
         */

        int n = s.length(); // length of s
        int m = t.length(); // length of t

        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }

        if (n > m) {
            // swap the input strings to consume less memory
            String tmp = s;
            s = t;
            t = tmp;
            n = m;
            m = t.length();
        }

        int p[] = new int[n+1]; //'previous' cost array, horizontally
        int d[] = new int[n+1]; // cost array, horizontally
        int _d[]; //placeholder to assist in swapping p and d

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t

        char t_j; // jth character of t

        int cost; // cost

        for (i = 0; i<=n; i++) {
            p[i] = i;
        }

        for (j = 1; j<=m; j++) {
            t_j = t.charAt(j-1);
            d[0] = j;

            for (i=1; i<=n; i++) {
                cost = s.charAt(i-1)==t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i-1]+1, p[i]+1),  p[i-1]+cost);
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now 
        // actually has the most recent cost counts
        return p[n];
    }


  
    public void addtoframe(Mat S,int X,int Y) {
	for(int i=0;i<S.width();i++)
		for(int j=0;j<S.height();j++)
			mRgba.put(j+X, i+Y, S.get(j, i));
	
}
    
    public Mat mainTextInitializr(String Text)
    {
        Mat kernel=Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(9,2));
        Mat closed=new Mat();
    	org.opencv.core.Rect rect=new org.opencv.core.Rect(new Point(50, 50), new Point(500,500));
        Mat R=new Mat(rect.height,rect.width,mRgba.type(),new Scalar(0,0,0,255));
        Core.putText(R, Text, new Point(0,R.height()/2),3 , 1, new Scalar(255,255,255,255),2);
        Imgproc.morphologyEx(R, closed, Imgproc.MORPH_CLOSE, kernel);
        List<MatOfPoint> contour = new ArrayList<MatOfPoint>();
        Imgproc.cvtColor(closed.clone(), closed, Imgproc.COLOR_RGB2GRAY);
        Imgproc.findContours(closed,contour,new Mat(),Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);
        double MaxArea=0;
        int ContourIndex=0;
        for(int i=0;i<contour.size();i++)
        {
        	MatOfPoint Temp=new MatOfPoint(contour.get(i));
        	rect= Imgproc.boundingRect(Temp);
        	if(rect.width*rect.height>MaxArea)
        	{
        		ContourIndex=i;
        		MaxArea=rect.width*rect.height;
        	}
        }
        MatOfPoint Temp=new MatOfPoint(contour.get(ContourIndex));
    	rect= Imgproc.boundingRect(Temp);
    	R=new Mat(R,rect);
    	Core.bitwise_not(R.clone(), R);
    	return R;
        
    }
    static double calcAngle(Mat  src)
    {
      int borderType;
      Scalar value; 
      
      Mat adapt_img = src;
      Mat adapt_img_padded = new Mat();
      Size size = src.size();
      /// Convert the image to Gray
      //cvtColor( src, src_gray, CV_RGB2GRAY );

      Mat lines=new Mat();

      int top = (int) (PAD *adapt_img.rows());
      int bottom = (int) (PAD *adapt_img.rows());
      int left = (int) (PAD *adapt_img.cols());
      int right = (int) (PAD *adapt_img.cols());
      borderType =Imgproc.BORDER_CONSTANT;
      value =new Scalar(0, 0, 0); // pad with all black
      //imshow("Before padding", adapt_img);

      Imgproc.copyMakeBorder (adapt_img, adapt_img_padded, top, bottom, left, right, borderType, value);
      //imshow("after padding", adapt_img_padded);
      Imgproc.HoughLinesP(adapt_img_padded, lines, 1,Math.PI/180, 100, size.width / 2.f, 20);

      double angle = 0.;
      for (int i = 0; i < lines.cols(); ++i)
      {
    	  double[] vec=lines.get(0, i);
    	  
          angle += Math.atan2(vec[3] - vec[1],vec[2] - vec[0]);
      }
      angle /= lines.cols(); // mean angle, in radians.
      return  angle * 180/Math.PI; // mean angle, in radians.
    }

    class DoBackGroundCalc extends AsyncTask<Void, Void, String>{
    	public Mat TestCaseTemp;

		@Override
		protected String doInBackground(Void... arg0) {
        	//Imgproc.threshold(TestCaseTemp.clone(), TestCaseTemp, 100, 255, Imgproc.THRESH_BINARY);

    		Bitmap bmp=null;
    		bmp=Bitmap.createBitmap(TestCaseTemp.cols(),TestCaseTemp.rows(),Bitmap.Config.ARGB_8888);
    		Utils.matToBitmap(TestCaseTemp, bmp);
    		baseApi.setImage(bmp);
    		String recognizedText = baseApi.getUTF8Text();
    		//baseApi.end();
    		return recognizedText;
		}
    }
   
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewStep1) {
            mViewMode = VIEW_MODE_Step1;
        } else if (item == mItemPreviewStep2) {
            mViewMode = VIEW_MODE_Step2;
        } else if (item == mItemPreviewStep3) {
            mViewMode = VIEW_MODE_Step3;
        } else if (item == mItemPreviewStep4) {
            mViewMode = VIEW_MODE_Step4;
        } else if (item == mItemPreviewStep5) {
            mViewMode = VIEW_MODE_Step5;
        }

        return true;
    }
    public native void FindFeatures(long matAddrGr, long matAddrRgba);
}
