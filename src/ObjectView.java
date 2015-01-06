package nz.gen.geek_central.ObjViewer;
/*
    3D view widget. Lets the user apply interactive rotation of the
    object around any axis. Also does rotation animations.

    Copyright 2011, 2013 by Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
*/

import javax.microedition.khronos.opengles.GL10;
import nz.gen.geek_central.GLUseful.ObjReader;
import nz.gen.geek_central.GLUseful.Rotation;
import android.graphics.PointF;
import android.view.MotionEvent;
import static nz.gen.geek_central.android.useful.Useful.GetTime;
import nz.gen.geek_central.android.useful.BundledSavedState;
import static nz.gen.geek_central.GLUseful.GLUseful.gl;

public class ObjectView extends android.opengl.GLSurfaceView
  {
    private static boolean DefaultUseLighting = true;
    private static boolean DefaultClockwiseFaces = false;
    private static Rotation DefaultRotation = Rotation.Null;

    private ObjReader.Model TheObject = null;
    private boolean UseLighting = DefaultUseLighting;
    private boolean ClockwiseFaces = DefaultClockwiseFaces;
    private Rotation CurRotation = DefaultRotation;
    private PointF LastMouse = null;

    private class ObjectViewRenderer implements Renderer
      {
      /* Note I ignore the passed GL10 argument, and exclusively use
        static methods from GLES11 class for all OpenGL drawing, since
        this seems to be the preferred way */

        public ObjectViewRenderer()
          {
            super();
          /* nothing else to do, really */
          } /*ObjectViewRenderer*/

        public void onDrawFrame
          (
            GL10 _gl
          )
          {
            gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
            gl.glMatrixMode(gl.GL_MODELVIEW);
            gl.glLoadIdentity();
            if (TheObject != null)
              {
                if (UseLighting)
                  {
                    gl.glEnable(gl.GL_LIGHTING);
                    gl.glLightModelfv
                      (
                        /*pname =*/ gl.GL_LIGHT_MODEL_AMBIENT,
                        /*params =*/ new float[] {0.3f, 0.3f, 0.3f, 1.0f},
                        /*offset =*/ 0
                      ); /* so hopefully objects are never completely black */
                  /* light positions are fixed relative to view */
                    gl.glEnable(gl.GL_LIGHT0);
                    gl.glLightfv
                      (
                        /*light =*/ gl.GL_LIGHT0,
                        /*pname =*/ gl.GL_POSITION,
                        /*params =*/ new float[] {0.0f, 2.0f, 0.0f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ gl.GL_LIGHT0,
                        /*pname =*/ gl.GL_AMBIENT,
                        /*params =*/ new float[] {0.4f, 0.4f, 0.4f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ gl.GL_LIGHT0,
                        /*pname =*/ gl.GL_DIFFUSE,
                        /*params =*/ new float[] {0.7f, 0.7f, 0.7f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ gl.GL_LIGHT0,
                        /*pname =*/ gl.GL_SPECULAR,
                        /*params =*/ new float[] {0.7f, 0.7f, 0.7f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glEnable(gl.GL_LIGHT1);
                    gl.glLightfv
                      (
                        /*light =*/ gl.GL_LIGHT1,
                        /*pname =*/ gl.GL_POSITION,
                        /*params =*/ new float[] {0.0f, 0.0f, -2.0f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ gl.GL_LIGHT1,
                        /*pname =*/ gl.GL_DIFFUSE,
                        /*params =*/ new float[] {0.3f, 0.3f, 0.3f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ gl.GL_LIGHT1,
                        /*pname =*/ gl.GL_SPECULAR,
                        /*params =*/ new float[] {0.3f, 0.3f, 0.3f, 1.0f},
                        /*offset =*/ 0
                      );
                  }
                else
                  {
                    gl.glDisable(gl.GL_LIGHTING);
                  } /*if*/
                final float MaxDim =
                    Math.max
                      (
                        Math.max
                          (
                            TheObject.BoundMax.x - TheObject.BoundMin.x,
                            TheObject.BoundMax.y - TheObject.BoundMin.y
                          ),
                        TheObject.BoundMax.z - TheObject.BoundMin.z
                      );
                final float Scale = 2.5f;
                gl.glTranslatef(0.0f, 0.0f, -2.5f);
                CurRotation.Apply();
                gl.glScalef(Scale / MaxDim, Scale / MaxDim, Scale / MaxDim);
                gl.glTranslatef
                  (
                    - (TheObject.BoundMax.x + TheObject.BoundMin.x) / 2.0f,
                    - (TheObject.BoundMax.y + TheObject.BoundMin.y) / 2.0f,
                    - (TheObject.BoundMax.z + TheObject.BoundMin.z) / 2.0f
                  );
                gl.glFrontFace
                  (
                    ClockwiseFaces ?
                        gl.GL_CW
                    :
                        gl.GL_CCW
                  );
                TheObject.Draw();
              } /*if*/
          } /*onDrawFrame*/

        public void onSurfaceChanged
          (
            GL10 _gl,
            int ViewWidth,
            int ViewHeight
          )
          {
            gl.glViewport(0, 0, ViewWidth, ViewHeight);
            gl.glMatrixMode(gl.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf
              (
                /*l =*/ ViewWidth > ViewHeight ? - 1.0f : - (float)ViewWidth / ViewHeight,
                /*r =*/ ViewWidth > ViewHeight ? 1.0f : (float)ViewWidth / ViewHeight,
                /*b =*/ ViewWidth < ViewHeight ? -1.0f : - (float)ViewHeight / ViewWidth,
                /*t =*/ ViewWidth < ViewHeight ? 1.0f : (float)ViewHeight / ViewWidth,
                /*n =*/ 1.0f,
                /*f =*/ 10.0f
              );
          } /*onSurfaceChanged*/

        public void onSurfaceCreated
          (
            GL10 _gl,
            javax.microedition.khronos.egl.EGLConfig Config
          )
          {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            gl.glEnable(gl.GL_CULL_FACE);
            gl.glShadeModel(gl.GL_SMOOTH);
            gl.glEnable(gl.GL_DEPTH_TEST);
          } /*onSurfaceCreated*/

      } /*ObjectViewRenderer*/

    public ObjectView
      (
        android.content.Context TheContext,
        android.util.AttributeSet TheAttributes
      )
      {
        super(TheContext, TheAttributes);
        setRenderer(new ObjectViewRenderer());
        setRenderMode(RENDERMODE_WHEN_DIRTY);
      } /*ObjectView*/

    private class RotationAnimator implements Runnable
      {
        final android.view.animation.Interpolator AnimFunction;
        final double StartTime, EndTime;
        final Rotation StartRotation, DeltaRotation;

        public RotationAnimator
          (
            android.view.animation.Interpolator AnimFunction,
            double Duration,
            Rotation EndRotation
          )
          {
            this.AnimFunction = AnimFunction;
            this.StartTime = GetTime();
            this.EndTime = this.StartTime + Duration;
            this.StartRotation = CurRotation;
            this.DeltaRotation = EndRotation.mul(StartRotation.inv());
            CurrentAnim = this;
            post(this);
          } /*RotationAnimator*/

        public void run()
          {
            if (CurrentAnim == this)
              {
                final double CurrentTime = GetTime();
                final float AnimAmt =
                    AnimFunction.getInterpolation((float)((CurrentTime - StartTime) / (EndTime - StartTime)));
                CurRotation = DeltaRotation.mul(AnimAmt).mul(StartRotation);
                requestRender();
                if (CurrentTime < EndTime)
                  {
                    post(this);
                  }
                else
                  {
                    CurrentAnim = null;
                  } /*if*/
              } /*if*/
          } /*run*/

      } /*RotationAnimator*/

    private RotationAnimator CurrentAnim = null;

    @Override
    public android.os.Parcelable onSaveInstanceState()
      {
        final android.os.Bundle MyState = new android.os.Bundle();
        MyState.putBoolean("UseLighting", UseLighting);
        MyState.putBoolean("ClockwiseFaces", ClockwiseFaces);
        MyState.putParcelable("CurRotation", CurRotation);
        return
            new BundledSavedState
              (
                super.onSaveInstanceState(),
                MyState
              );
      } /*onSaveInstanceState*/

    @Override
    public void onRestoreInstanceState
      (
        android.os.Parcelable SavedState
      )
      {
        super.onRestoreInstanceState(((BundledSavedState)SavedState).SuperState);
        final android.os.Bundle MyState = ((BundledSavedState)SavedState).MyState;
        UseLighting = MyState.getBoolean("UseLighting", DefaultUseLighting);
        ClockwiseFaces = MyState.getBoolean("ClockwiseFaces", DefaultClockwiseFaces);
        CurRotation = (Rotation)MyState.getParcelable("CurRotation");
        requestRender();
      } /*onRestoreInstanceState*/

    private Rotation CalculateNewRotation
      (
        PointF LastMouse,
        PointF ThisMouse,
        boolean Fling /* debug */
      )
      {
        final PointF MidPoint = new PointF(getWidth() / 2.0f, getHeight() / 2.0f);
        final float Radius =
            (float)Math.hypot(ThisMouse.x - MidPoint.x, ThisMouse.y - MidPoint.y);
        final float DeltaR =
                Radius
            -
                (float)Math.hypot(LastMouse.x - MidPoint.x, LastMouse.y - MidPoint.y);
          /* radial movement, for rotation about X and Y axes */
        final float MidDiag = (float)Math.hypot(MidPoint.x, MidPoint.y);
        final float MaxRot = (float)Math.PI; /* impose limit in case of wild fling gestures */
        final float ZAngle =
            Radius / MidDiag >= 0.5f ?
                (float)
                  (
                        Math.atan2
                          (
                            ThisMouse.y - MidPoint.y,
                            ThisMouse.x - MidPoint.x
                          )
                    -
                        Math.atan2
                          (
                            LastMouse.y - MidPoint.y,
                            LastMouse.x - MidPoint.x
                          )
                  )
            : /* disable Z-rotation too close to centre where it’s too hard to control */
                0.0f;
        final float RotationSin = DeltaR / MidDiag;
          /* scale rotation angle by assuming depth of
            axis is equal to radius of view */
          { /* debug */
            final double RotationAngle =
                Math.toDegrees
                  (
                    Math.abs(RotationSin) > 1.0f ?
                      /* can happen with fling gestures */
                        RotationSin * (float)Math.PI / 2.0f
                    :
                        (float)Math.asin(RotationSin)
                  );
        System.err.printf("ObjViewer.ObjectView.CalculateNewRotation fling %s LastMouse(%.3f, %.3f) × ThisMouse(%.3f, %.3f) => rotation %.3f° about (%.3f, %.3f, 0)°\n", Fling, LastMouse.x, LastMouse.y, ThisMouse.x, ThisMouse.y, RotationAngle, (ThisMouse.y - MidPoint.y) / Radius, (ThisMouse.x - MidPoint.x) / Radius);
          } /* debug */
        return
                new Rotation /* X+Y axis */
                  (
                    Math.abs(RotationSin) > 1.0f ?
                      /* can happen with fling gestures */
                        Math.max(Math.min(RotationSin * (float)Math.PI / 2.0f, MaxRot), - MaxRot)
                    :
                        (float)Math.asin(RotationSin),
                    false,
                    (ThisMouse.y - MidPoint.y) / Radius,
                    (ThisMouse.x - MidPoint.x) / Radius,
                    0
                  )
            .mul
              (
                new Rotation(Math.max(Math.min(ZAngle, MaxRot), - MaxRot), false, 0, 0, -1) /* Z axis */
              )
            .mul
              (
                CurRotation
              );
              /* ordering of composing the new rotations doesn't matter
                because axes are orthogonal */
      } /*CalculateNewRotation*/

    private final android.view.GestureDetector FlingDetector =
        new android.view.GestureDetector
          (
            getContext(),
            new android.view.GestureDetector.SimpleOnGestureListener()
              {
                @Override
                public boolean onFling
                  (
                    MotionEvent DownEvent,
                    MotionEvent UpEvent,
                    float XVelocity,
                    float YVelocity
                  )
                  {
                    final double CurrentTime = GetTime();
                    final float InitialAttenuate = 5.0f; /* attenuates initial speed */
                    final float FinalAttenuate = 2.0f; /* attenuates duration of spin */
                    final float SpinDuration =
                            (float)Math.hypot(XVelocity, YVelocity)
                        /
                            (float)Math.hypot(getWidth(), getHeight())
                        /
                            FinalAttenuate;
                    System.err.printf("ObjViewer.ObjectView: spin duration = %.3fs\n", SpinDuration); /* debug */
                    final PointF LastMouse = new PointF(UpEvent.getX(), UpEvent.getY());
                    final PointF ThisMouse =
                            new PointF
                              (
                                    LastMouse.x
                                +
                                        XVelocity
                                    *
                                        SpinDuration
                                    /
                                        InitialAttenuate,
                                    LastMouse.y
                                +
                                        YVelocity
                                    *
                                        SpinDuration
                                    /
                                        InitialAttenuate
                              );
                    new RotationAnimator
                      (
                        /*AnimFunction =*/ new android.view.animation.DecelerateInterpolator(),
                        /*Duration =*/ SpinDuration,
                        /*EndRotation =*/ CalculateNewRotation(LastMouse, ThisMouse, true)
                      );
                    return
                        true;
                  } /*onFling*/
              } /*GestureDetector.SimpleOnGestureListener*/
          );

    @Override
    public boolean onTouchEvent
      (
        MotionEvent TheEvent
      )
      {
        boolean Handled = false;
        if (CurrentAnim == null && FlingDetector.onTouchEvent(TheEvent))
          {
            Handled = true;
          } /*if*/
        if (!Handled)
          {
            switch (TheEvent.getAction())
              {
            case MotionEvent.ACTION_DOWN:
                CurrentAnim = null;
                LastMouse = new PointF(TheEvent.getX(), TheEvent.getY());
                Handled = true;
            break;
            case MotionEvent.ACTION_MOVE:
                if (LastMouse != null && TheObject != null)
                  {
                    final PointF ThisMouse = new PointF(TheEvent.getX(), TheEvent.getY());
                    CurRotation = CalculateNewRotation(LastMouse, ThisMouse, false);
                    LastMouse = ThisMouse;
                    requestRender();
                  } /*if*/
                Handled = true;
            break;
            case MotionEvent.ACTION_UP:
                LastMouse = null;
                Handled = true;
            break;
              } /*switch*/
          } /*if*/
        return
            Handled;
      } /*onTouchEvent*/

    public void ResetOrientation
      (
        boolean Animate
      )
      {
        SetOrientation(Rotation.Null, Animate);
      } /*ResetOrientation*/

    public void SetObject
      (
        ObjReader.Model NewObject
      )
      {
        TheObject = NewObject;
        ResetOrientation(false);
      } /*SetObject*/

    public ObjReader.Model GetObject()
      {
        return
            TheObject;
      } /*GetObject*/

    public void SetUseLighting
      (
        boolean UseLighting
      )
      {
        this.UseLighting = UseLighting;
        requestRender();
      } /*SetUseLighting*/

    public boolean GetUseLighting()
      {
        return
            UseLighting;
      } /*GetUseLighting*/

    public void SetClockwiseFaces
      (
        boolean ClockwiseFaces
      )
      {
        this.ClockwiseFaces = ClockwiseFaces;
        requestRender();
      } /*SetClockwiseFaces*/

    public boolean GetClockwiseFaces()
      {
        return
            ClockwiseFaces;
      } /*GetClockwiseFaces*/

    public void SetOrientation
      (
        Rotation NewOrientation,
        android.view.animation.Interpolator AnimFunction, /* optional */
        float AnimDuration /* ignored unless AnimFunction is specified */
      )
      {
        if (AnimFunction != null)
          {
            new RotationAnimator
              (
                /*AnimFunction =*/ AnimFunction,
                /*Duration =*/ AnimDuration,
                /*EndRotation =*/ NewOrientation
              );
          }
        else
          {
            CurRotation = NewOrientation;
            requestRender();
          } /*if*/
      } /*SetOrientation*/

    public void SetOrientation
      (
        Rotation NewOrientation,
        boolean Animate
      )
      {
        SetOrientation
          (
            /*NewOrientation =*/ NewOrientation,
            /*AnimFunction =*/
                Animate ?
                    new PolyAccelerationDecelerationInterpolator(3.0f)
                :
                    null,
            /*AnimDuration =*/ 1.5f
          );
      } /*SetOrientation*/

  } /*ObjectView*/
