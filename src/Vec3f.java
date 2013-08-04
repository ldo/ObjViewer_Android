package nz.gen.geek_central.GLUseful;
/*
    functional 3D vector operations

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

public class Vec3f
  /* 3D vectors */
  {
    public final float x, y, z, w;

    public Vec3f
      (
        float x,
        float y,
        float z
      )
      {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = 1.0f;
      } /*Vec3f*/

    public Vec3f
      (
        float x,
        float y,
        float z,
        float w
      )
      {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
      } /*Vec3f*/

    public Vec3f
      (
        float[] v
      )
      {
        if (v.length != 3 && v.length != 4)
          {
            throw new RuntimeException("need 3 or 4 floats to make a vector");
          } /*if*/
        x = v[0];
        y = v[1];
        z = v[2];
        w = v.length == 4 ? v[3] : 1.0f;
      } /*Vec3f*/

    public float[] to_floats
      (
        int nrelts /* 3 or 4 */
      )
      {
        float[] v;
        switch (nrelts)
          {
        case 3:
            v = new float[] {x, y, z};
        break;
        case 4:
            v = new float[] {x, y, z, w};
        break;
        default:
            throw new RuntimeException("vector can only convert to 3 or 4 floats");
      /* break; */
          } /*switch*/
        return
            v;
      } /*to_floats*/

    public static Vec3f zero()
      {
        return
            new Vec3f(0.0f, 0.0f, 0.0f);
      } /*zero*/

    public Vec3f neg()
      {
        return
            new Vec3f(-x, -y, -z, w);
      } /*neg*/

    public Vec3f add
      (
        Vec3f v
      )
      {
        return
            new Vec3f(x + v.x, y + v.y, z + v.z);
      } /*add*/

    public Vec3f sub
      (
        Vec3f v
      )
      {
        return
            new Vec3f(x - v.x, y - v.y, z - v.z);
      } /*sub*/

    public Vec3f mul
      (
        float s
      )
      {
        return
            new Vec3f(x * s, y * s, z * s, w);
      } /*mul*/

    public Vec3f recip()
      {
        return
            new Vec3f(1.0f / x, 1.0f / y, 1.0f / z);
      } /*recip*/

    public float dot
      (
        Vec3f v
      )
      {
        return
            v.x * this.x + v.y * this.y + v.z * this.z;
      } /*dot*/

    public Vec3f cross
      (
        Vec3f v
      )
      {
        return
            new Vec3f
              (
                this.y * v.z - v.y * this.z,
                this.z * v.x - v.z * this.x,
                this.x * v.y - v.x * this.y
              );
      } /*cross*/

    public float azimuth()
        /* returns the angle between the x-axis and the line from the origin to the point. */
      {
        return
            (float)Math.atan2(y, x);
      } /*azimuth*/

    public float elevation()
        /* returns the angle between the x-y plane and the line from the origin to the point. */
      {
        return
            (float)Math.atan2(z, (float)Math.sqrt(x * x + y * y));
      } /*elevation*/

    public float abs()
        /* returns the distance between the point and the origin. */
      {
        return
            (float)Math.sqrt((x * x + y * y + z * z) / (w * w));
      } /*abs*/

    public Vec3f unit()
      {
        final float abs = this.abs();
        return
            new Vec3f(x / abs, y / abs, z / abs);
      } /*unit*/

    public Vec3f norm()
      /* rescales so w = 1. */
      {
        return
            new Vec3f(x / w, y / w, z / w);
      } /*norm*/

  } /*Vec3f*/
