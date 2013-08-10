package nz.gen.geek_central.GLUseful;
/*
    Quaternion representation of 3D rotation transformations.

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

import static nz.gen.geek_central.GLUseful.GLUseful.gl;

public class Rotation implements android.os.Parcelable
  {
    public final float c, x, y, z;

    public Rotation
      (
        float Angle,
        boolean Degrees, /* false for radians */
        float X,
        float Y,
        float Z
      )
      /* constructs a Rotation that rotates by the specified angle
        about the axis direction (X, Y, Z). */
      {
        final double Theta = (Degrees ? Math.toRadians(Angle) : Angle) / 2;
        final float Cos = (float)Math.cos(Theta);
        final float Sin = (float)Math.sin(Theta);
        final float Mag = (float)Math.sqrt(X * X + Y * Y + Z * Z);
        c = Cos;
        x = Sin * X / Mag;
        y = Sin * Y / Mag;
        z = Sin * Z / Mag;
      } /*Rotation*/

    public Rotation
      (
        float Angle,
        boolean Degrees, /* false for radians */
        Vec3f Axis
      )
      {
        this(Angle, Degrees, Axis.x, Axis.y, Axis.z);
      } /*Rotation*/

    public static final Rotation Null = new Rotation(0, 0, 0, 1);
      /* represents no rotation at all */

    private Rotation
      (
        float c,
        float x,
        float y,
        float z
      )
      /* internal-use constructor with directly-computed components. Note
        this does not compensate for accumulated rounding errors. */
      {
        this.c = c;
        this.x = x;
        this.y = y;
        this.z = z;
      } /*Rotation*/

    public static final android.os.Parcelable.Creator<Rotation> CREATOR =
      /* restore state from a Parcel. */
        new android.os.Parcelable.Creator<Rotation>()
          {
            public Rotation createFromParcel
              (
                android.os.Parcel Post
              )
              {
                final android.os.Bundle MyState = Post.readBundle();
                return
                    new Rotation
                      (
                        MyState.getFloat("c", Null.c),
                        MyState.getFloat("x", Null.x),
                        MyState.getFloat("y", Null.y),
                        MyState.getFloat("z", Null.z)
                      );
              } /*createFromParcel*/

            public Rotation[] newArray
              (
                int NrElts
              )
              {
                return
                    new Rotation[NrElts];
              } /*newArray*/
          } /*Parcelable.Creator*/;

    @Override
    public int describeContents()
      {
        return
            0; /* nothing special */
      } /*describeContents*/

    @Override
    public void writeToParcel
      (
        android.os.Parcel Post,
        int Flags
      )
      /* save state to a Parcel. */
      {
        final android.os.Bundle MyState = new android.os.Bundle();
        MyState.putFloat("c", c);
        MyState.putFloat("x", x);
        MyState.putFloat("y", y);
        MyState.putFloat("z", z);
        Post.writeBundle(MyState);
      } /*writeToParcel*/

    public Rotation inv()
      /* returns rotation by the opposite angle around the same axis. Or alternatively,
        the same angle around the opposite-pointing axis . */
      {
        return
            new Rotation(c, -x, -y, -z);
      } /*inv*/

    public Rotation mul
      (
        Rotation that
      )
      /* returns composition with another rotation. */
      {
        return
            new Rotation
              (
                this.c * that.c - this.x * that.x - this.y * that.y - this.z * that.z,
                this.y * that.z - this.z * that.y + this.c * that.x + that.c * this.x,
                this.z * that.x - this.x * that.z + this.c * that.y + that.c * this.y,
                this.x * that.y - this.y * that.x + this.c * that.z + that.c * this.z
              );
      } /*mul*/

    public Rotation mul
      (
        float Frac
      )
      /* returns the specified fraction of the rotation. */
      {
        final float Mag = (float)Math.sqrt(x * x + y * y + z * z);
        return
            Mag != 0.0f ?
                new Rotation
                  (
                    GetAngle(false) * Frac, false, x / Mag, y / Mag, z / Mag
                  )
            :
                Null;
      } /*mul*/

    public float GetAngle
      (
        boolean Degrees /* false for radians */
      )
      /* returns the rotation angle. */
      {
        final double Theta = Math.atan2(Math.sqrt(x * x + y * y + z * z), c);
        return
            2 * (float)(Degrees ? Math.toDegrees(Theta) : Theta);
      } /*GetAngle*/

    public Vec3f GetAxis()
      {
        final float Mag = (float)Math.sqrt(x * x + y * y + z * z);
        return
            Mag != 0.0f ?
                new Vec3f(x / Mag, y / Mag, z / Mag)
            :
                new Vec3f(Null.x, Null.y, Null.z);
      } /*GetAxis*/

    public void Apply()
      /* applies the rotation to the currently-selected GL matrix. */
      {
        final float Mag = (float)Math.sqrt(x * x + y * y + z * z);
        if (Mag != 0.0f)
          {
            gl.glRotatef(2 * (float)Math.toDegrees(Math.atan2(Mag, c)), x / Mag, y / Mag, z / Mag);
          } /*if*/
      } /*Apply*/

    public String toString()
      {
        return
            String.format
              (
                GLUseful.StdLocale,
                "Rotation(%e, %e, %e, %e)",
                c, x, y, z
              );
      } /*toString*/

  } /*Rotation*/
