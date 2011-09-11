package nz.gen.geek_central.ObjViewer;
/*
    3D view widget. Lets the user apply interactive rotation of the
    object around any axis. Also does rotation animations.

    Copyright 2011 by Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

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

public class ObjectView extends android.opengl.GLSurfaceView
  {
    private static boolean DefaultUseLighting = false;
    private static boolean DefaultClockwiseFaces = false;
    private static Rotation DefaultRotation = Rotation.Null;

    private ObjReader.Model TheObject = null;
    private boolean UseLighting = DefaultUseLighting;
    private boolean ClockwiseFaces = DefaultClockwiseFaces;
    private Rotation CurRotation = DefaultRotation;
    private PointF LastMouse = null;

    private class ObjectViewRenderer implements Renderer
      {

        public ObjectViewRenderer()
          {
            super();
          /* nothing else to do, really */
          } /*ObjectViewRenderer*/

        public void onDrawFrame
          (
            GL10 gl
          )
          {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            if (TheObject != null)
              {
                if (UseLighting)
                  {
                    gl.glEnable(GL10.GL_LIGHTING);
                  /* light positions are fixed relative to view */
                    gl.glEnable(GL10.GL_LIGHT0);
                    gl.glLightfv
                      (
                        /*light =*/ GL10.GL_LIGHT0,
                        /*pname =*/ GL10.GL_POSITION,
                        /*params =*/ new float[] {0.0f, 2.0f, 0.0f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ GL10.GL_LIGHT0,
                        /*pname =*/ GL10.GL_AMBIENT,
                        /*params =*/ new float[] {0.4f, 0.4f, 0.4f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ GL10.GL_LIGHT0,
                        /*pname =*/ GL10.GL_DIFFUSE,
                        /*params =*/ new float[] {0.7f, 0.7f, 0.7f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ GL10.GL_LIGHT0,
                        /*pname =*/ GL10.GL_SPECULAR,
                        /*params =*/ new float[] {0.7f, 0.7f, 0.7f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glEnable(GL10.GL_LIGHT1);
                    gl.glLightfv
                      (
                        /*light =*/ GL10.GL_LIGHT1,
                        /*pname =*/ GL10.GL_POSITION,
                        /*params =*/ new float[] {0.0f, 0.0f, -2.0f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ GL10.GL_LIGHT1,
                        /*pname =*/ GL10.GL_DIFFUSE,
                        /*params =*/ new float[] {0.3f, 0.3f, 0.3f, 1.0f},
                        /*offset =*/ 0
                      );
                    gl.glLightfv
                      (
                        /*light =*/ GL10.GL_LIGHT1,
                        /*pname =*/ GL10.GL_SPECULAR,
                        /*params =*/ new float[] {0.3f, 0.3f, 0.3f, 1.0f},
                        /*offset =*/ 0
                      );
                  }
                else
                  {
                    gl.glDisable(GL10.GL_LIGHTING);
                  } /*if*/
                final float MaxDim =
                    (float)Math.max
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
                CurRotation.Apply(gl);
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
                        GL10.GL_CW
                    :
                        GL10.GL_CCW
                  );
                TheObject.Draw(gl);
              } /*if*/
          } /*onDrawFrame*/

        public void onSurfaceChanged
          (
            GL10 gl,
            int ViewWidth,
            int ViewHeight
          )
          {
            gl.glViewport(0, 0, ViewWidth, ViewHeight);
            gl.glMatrixMode(GL10.GL_PROJECTION);
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
            GL10 gl,
            javax.microedition.khronos.egl.EGLConfig Config
          )
          {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            gl.glEnable(GL10.GL_CULL_FACE);
            gl.glShadeModel(GL10.GL_SMOOTH);
            gl.glEnable(GL10.GL_DEPTH_TEST);
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
            double StartTime,
            double EndTime,
            Rotation StartRotation,
            Rotation EndRotation
          )
          {
            this.AnimFunction = AnimFunction;
            this.StartTime = StartTime;
            this.EndTime = EndTime;
            this.StartRotation = StartRotation;
            this.DeltaRotation = EndRotation.mul(StartRotation.inv());
            CurrentAnim = this;
            getHandler().post(this);
          } /*RotationAnimator*/

        public void run()
          {
            final double CurrentTime = System.currentTimeMillis() / 1000.0;
            final float AnimAmt =
                AnimFunction.getInterpolation((float)((CurrentTime - StartTime) / (EndTime - StartTime)));
            CurRotation = DeltaRotation.mul(AnimAmt).mul(StartRotation);
            requestRender();
            if (CurrentTime < EndTime)
              {
                getHandler().post(this);
              }
            else
              {
                CurrentAnim = null;
              } /*if*/
          } /*run*/

      } /*RotationAnimator*/

    private RotationAnimator CurrentAnim = null;

    protected static class SavedObjectViewState extends android.view.AbsSavedState
      /* for saving/restoring instance state */
      {
        public static android.os.Parcelable.Creator<SavedObjectViewState> CREATOR =
            new android.os.Parcelable.Creator<SavedObjectViewState>()
              {
                public SavedObjectViewState createFromParcel
                  (
                    android.os.Parcel SavedState
                  )
                  {
                    final android.view.AbsSavedState SuperState =
                        android.view.AbsSavedState.CREATOR.createFromParcel(SavedState);
                    final android.os.Bundle MyState = SavedState.readBundle();
                    return
                        new SavedObjectViewState
                          (
                            SuperState,
                            MyState.getBoolean("UseLighting", DefaultUseLighting),
                            MyState.getBoolean("ClockwiseFaces", DefaultClockwiseFaces),
                            (Rotation)MyState.getParcelable("CurRotation")
                          );
                  } /*createFromParcel*/

                public SavedObjectViewState[] newArray
                  (
                    int NrElts
                  )
                  {
                    return
                        new SavedObjectViewState[NrElts];
                  } /*newArray*/
              } /*Parcelable.Creator*/;

        public final android.os.Parcelable SuperState;
      /* state that I'm actually interested in saving/restoring: */
        public final boolean UseLighting;
        public final boolean ClockwiseFaces;
        public final Rotation CurRotation;

        public SavedObjectViewState
          (
            android.os.Parcelable SuperState,
            boolean UseLighting,
            boolean ClockwiseFaces,
            Rotation CurRotation
          )
          {
            super(SuperState);
            this.SuperState = SuperState;
            this.UseLighting = UseLighting;
            this.ClockwiseFaces = ClockwiseFaces;
            this.CurRotation = CurRotation;
          } /*SavedObjectViewState*/

        public void writeToParcel
          (
            android.os.Parcel SavedState,
            int Flags
          )
          {
            super.writeToParcel(SavedState, Flags);
          /* put my state in a Bundle, where each item is associated with a
            keyword name (unlike the Parcel itself, where items are identified
            by order). I think this makes things easier to understand. */
            final android.os.Bundle MyState = new android.os.Bundle();
            MyState.putBoolean("UseLighting", UseLighting);
            MyState.putBoolean("ClockwiseFaces", ClockwiseFaces);
            MyState.putParcelable("CurRotation", CurRotation);
            SavedState.writeBundle(MyState);
          } /*writeToParcel*/

      } /*SavedObjectViewState*/

    @Override
    public android.os.Parcelable onSaveInstanceState()
      {
        return
            new SavedObjectViewState
              (
                super.onSaveInstanceState(),
                UseLighting,
                ClockwiseFaces,
                CurRotation
              );
      } /*onSaveInstanceState*/

    @Override
    public void onRestoreInstanceState
      (
        android.os.Parcelable SavedState
      )
      {
        final SavedObjectViewState MyState = (SavedObjectViewState)SavedState;
        super.onRestoreInstanceState(MyState.SuperState);
        UseLighting = MyState.UseLighting;
        ClockwiseFaces = MyState.ClockwiseFaces;
        CurRotation = MyState.CurRotation;
        requestRender();
      } /*onRestoreInstanceState*/

    @Override
    public boolean onTouchEvent
      (
        MotionEvent TheEvent
      )
      {
        boolean Handled = false;
        if (CurrentAnim == null)
          {
           /* fling gestures? */
            switch (TheEvent.getAction())
              {
            case MotionEvent.ACTION_DOWN:
                LastMouse = new PointF(TheEvent.getX(), TheEvent.getY());
                Handled = true;
            break;
            case MotionEvent.ACTION_MOVE:
                if (LastMouse != null && TheObject != null)
                  {
                    final PointF ThisMouse = new PointF(TheEvent.getX(), TheEvent.getY());
                    final PointF MidPoint = new PointF(getWidth() / 2.0f, getHeight() / 2.0f);
                    final float Radius =
                        (float)Math.hypot(ThisMouse.x - MidPoint.x, ThisMouse.y - MidPoint.y);
                    final float DeltaR =
                            Radius
                        -
                            (float)Math.hypot(LastMouse.x - MidPoint.x, LastMouse.y - MidPoint.y);
                      /* radial movement, for rotation about X and Y axes */
                    final float ZAngle =
                        Radius / (float)Math.hypot(MidPoint.x, MidPoint.y) >= 0.5 ?
                            (float)Math.toDegrees
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
                    CurRotation =
                            new Rotation /* X+Y axis */
                              (
                                (float)Math.toDegrees
                                  (
                                    Math.asin
                                      (
                                            DeltaR
                                        /
                                            (float)Math.hypot(MidPoint.x, MidPoint.y)
                                              /* scale rotation angle by assuming depth of
                                                axis is equal to radius of view */
                                      )
                                  ),
                                (ThisMouse.y - MidPoint.y) / Radius,
                                (ThisMouse.x - MidPoint.x) / Radius,
                                0
                              )
                        .mul
                          (
                            new Rotation(ZAngle, 0, 0, -1) /* Z axis */
                          )
                        .mul
                          (
                            CurRotation
                          );
                          /* ordering of composing the new rotations doesn't matter
                            because axes are orthogonal */
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
            final double CurrentTime = System.currentTimeMillis() / 1000.0;
            new RotationAnimator
              (
                /*AnimFunction =*/ AnimFunction,
                /*StartTime =*/ CurrentTime,
                /*EndTime =*/ CurrentTime + AnimDuration,
                /*StartRotation =*/ CurRotation,
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
