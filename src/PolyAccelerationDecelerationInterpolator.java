package nz.gen.geek_central.ObjViewer;
/*
    Example animation interpolator that implements polynomial acceleration
    and deceleration.

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

public class PolyAccelerationDecelerationInterpolator
    implements android.view.animation.Interpolator
  {
    public final float Power;
      /* the higher the power, the more relative time is spent accelerating
        and decelerating, and so the faster the maximum speed is */

    public PolyAccelerationDecelerationInterpolator
      (
        float Power
      )
      {
        this.Power = Power;
      } /*PolyAccelerationDecelerationInterpolator*/

    public float getInterpolation
      (
        float RelTime
      )
      {
        final float Offset = RelTime > 0.5f ? 1.0f - RelTime : RelTime;
        final float Y = (float)Math.pow(2.0f * Offset, Power + 1.0f) / 2.0f;
        return
            RelTime > 0.5f ? 1.0f - Y : Y;
      } /*getInterpolation*/

  } /*PolyAccelerationDecelerationInterpolator*/
