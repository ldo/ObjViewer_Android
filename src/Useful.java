package nz.gen.geek_central.android.useful;
/*
    Some basic useful stuff.

    Copyright 2013 by Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

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

public class Useful
  {

    public static double GetTime()
      /* returns current UTC time in seconds. */
      {
        return
            System.currentTimeMillis() / 1000.0;
      } /*GetTime*/

  } /*Useful*/;
