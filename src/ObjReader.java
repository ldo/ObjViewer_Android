package nz.gen.geek_central.GLUseful;
/*
    Basic reader for .obj 3D model files.

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

import java.util.ArrayList;

public class ObjReader
  {

    public static class DataFormatException extends RuntimeException
      /* indicates a problem parsing a .obj file. */
      {

        public DataFormatException
          (
            String Message
          )
          {
            super(Message);
          } /*DataFormatException*/

      } /*DataFormatException*/

    private static class ObjTokenizer
      {
        private final java.io.InputStream Input;
        public boolean EOL, EOF;
        private boolean LastWasCR, InComment;
        public int LineNr, ColNr;

        public void Fail
          (
            String Msg
          )
          {
            throw new DataFormatException
              (
                String.format
                  (
                    "ObjReader error at line %d, col %d: %s",
                    LineNr, ColNr,
                    Msg
                  )
              );
          } /*Fail*/

        public ObjTokenizer
          (
            java.io.InputStream Input
          )
          {
            this.Input = Input;
            EOL = false;
            EOF = false;
            LastWasCR = false;
            InComment = false;
            LineNr = 1;
            ColNr = 0;
          } /*ObjTokenizer*/

        private boolean IsSeparator
          (
            char Ch
          )
          {
            return
                Ch <= ' ';
          } /*IsSeparator*/

        private boolean IsEOL
          (
            char Ch
          )
          {
            return
                Ch == '\015' || Ch == '\012';
          } /*IsEOL*/

        private char NextCh()
          {
            if (EOF)
              {
                Fail("read past EOF");
              } /*if*/
            ++ColNr;
            /*final*/ char Result
                = (char)-1; /*sigh*/
            try
              {
                for (;;)
                  {
                    final int ich = Input.read();
                    if (ich < 0)
                      {
                        EOF = true;
                        Result = '\n';
                        break;
                      }
                    else if (LastWasCR && (char)ich == '\012')
                      {
                      /* skip LF following CR */
                        LastWasCR = false;
                      }
                    else
                      {
                        Result = (char)ich;
                        break;
                      } /*if*/
                  } /*for*/
              }
            catch (java.io.IOException IOError)
              {
                Fail("input error: " + IOError.toString());
              } /*try*/
            EOL = EOF || IsEOL(Result);
            LastWasCR = Result == '\015';
            return
                Result;
          } /*NextCh*/

        public String NextSym
          (
            boolean Required
          )
          /* fetches next symbol from current line, or null if none. */
          {
            final StringBuilder CurSym = new StringBuilder();
            for (;;)
              {
                if (EOL)
                    break;
                final char Ch = NextCh();
                if (!InComment)
                  {
                    if (IsSeparator(Ch))
                      {
                        if (CurSym.length() != 0)
                            break;
                      }
                    else if (Ch == '#')
                      {
                        InComment = true;
                      }
                    else
                      {
                        CurSym.appendCodePoint((int)Ch);
                      } /*if*/
                  } /*if*/
              } /*for*/
            final String Result =
                CurSym.length() != 0 ?
                    CurSym.toString()
                :
                    null;
            if (Result == null && Required)
              {
                Fail("missing required symbol");
              } /*if*/
            return
                Result;
          } /*NextSym*/

        public void EndLine()
          /* skips rest of current line (must be nothing more than whitespace left). */
          {
            for (;;)
              {
                if (EOL)
                    break;
                final char Ch = NextCh();
                if (IsEOL(Ch))
                    break;
                if (!InComment && !IsSeparator(Ch))
                  {
                    Fail("unexpected stuff at end of line");
                  } /*if*/
              } /*for*/
            EOL = false;
            if (!EOF)
              {
                ++LineNr;
                ColNr = 0;
              } /*if*/
            InComment = false;
          } /*EndLine*/

        public GeomBuilder.Vec3f GetVec()
          {
            final String XStr = NextSym(true);
            final String YStr = NextSym(true);
            final String ZStr = NextSym(true);
            /*final*/ GeomBuilder.Vec3f Result
                = null; /*sigh*/
            try
              {
                Result = new GeomBuilder.Vec3f
                  (
                    Float.parseFloat(XStr),
                    Float.parseFloat(YStr),
                    Float.parseFloat(ZStr)
                  );
              }
            catch (NumberFormatException BadNum)
              {
                Fail("bad vector coordinate");
              } /*catch*/
            return
                Result;
          } /*GetVec*/

        public void SkipRest()
          {
            while (!EOL)
              {
                NextCh();
              } /*while*/
          } /*SkipRest*/

      } /*ObjTokenizer*/

    private static class FaceVert
      {
        public final Integer VertIndex, TexCoordIndex, NormalIndex;

        public FaceVert
          (
            Integer VertIndex, /* mandatory */
            Integer TexCoordIndex, /* optional */
            Integer NormalIndex /* optional */
          )
          {
            this.VertIndex = VertIndex;
            this.TexCoordIndex = TexCoordIndex;
            this.NormalIndex = NormalIndex;
          } /*FaceVert*/

      } /*FaceVert*/

    public static GeomBuilder.Obj Read
      (
        java.io.InputStream From
      )
    throws DataFormatException
      {
        final ObjTokenizer Parse = new ObjTokenizer(From);
        ArrayList<GeomBuilder.Vec3f>
            Vertices = null,
            TexCoords = null,
            Normals = null;
        ArrayList<FaceVert[]> Faces = null;
        for (;;)
          {
            final String OpStr = Parse.NextSym(false);
            if (OpStr != null)
              {
                final String Op = OpStr.intern();
                if (Op == "v")
                  {
                    final GeomBuilder.Vec3f Vec = Parse.GetVec();
                    if (Vertices == null)
                      {
                        Vertices = new ArrayList<GeomBuilder.Vec3f>();
                      } /*if*/
                    Vertices.add(Vec);
                  }
                else if (Op == "vt")
                  {
                    final GeomBuilder.Vec3f Vec = Parse.GetVec();
                    if (TexCoords == null)
                      {
                        TexCoords = new ArrayList<GeomBuilder.Vec3f>();
                      } /*if*/
                    TexCoords.add(Vec);
                  }
                else if (Op == "vn")
                  {
                    final GeomBuilder.Vec3f Vec = Parse.GetVec();
                    if (Normals == null)
                      {
                        Normals = new ArrayList<GeomBuilder.Vec3f>();
                      } /*if*/
                    Normals.add(Vec);
                  }
                else if (Op == "f")
                  {
                    if (Faces == null)
                      {
                        Faces = new ArrayList<FaceVert[]>();
                      } /*if*/
                    final ArrayList<FaceVert> FaceVerts = new ArrayList<FaceVert>();
                    for (;;)
                      {
                        final String VertStr = Parse.NextSym(false);
                        if (VertStr == null)
                            break;
                        Integer VertIndex = null, TexCoordIndex = null, NormalIndex = null;
                        int Which = 0, ThisPos = 0, LastPos;
                        for (;;)
                          {
                            LastPos = ThisPos;
                            for (;;)
                              {
                                if (ThisPos == VertStr.length())
                                    break;
                                if (VertStr.charAt(ThisPos) == '/')
                                    break;
                                ++ThisPos;
                              } /*for*/
                            if (ThisPos > LastPos)
                              {
                                int ThisComponent
                                    = -1; /*sigh*/
                                try
                                  {
                                    ThisComponent = Integer.parseInt
                                      (
                                        VertStr.substring(LastPos, ThisPos)
                                      );
                                  }
                                catch (NumberFormatException BadNum)
                                  {
                                    Parse.Fail("bad vertex index");
                                  } /*catch*/
                                switch (Which)
                                  {
                                case 0:
                                    if (Vertices == null)
                                      {
                                        Parse.Fail("vertices referenced but not defined");
                                      } /*if*/
                                    if (ThisComponent < 0)
                                      {
                                        ThisComponent += Vertices.size();
                                      }
                                    else
                                      {
                                        ThisComponent -= 1;
                                      } /*if*/
                                    if (ThisComponent < 0 || ThisComponent >= Vertices.size())
                                      {
                                        Parse.Fail("vertex reference out of range");
                                      } /*if*/
                                    VertIndex = ThisComponent;
                                break;
                                case 1:
                                    if (TexCoords == null)
                                      {
                                        Parse.Fail("texcoords referenced but not defined");
                                      } /*if*/
                                    if (ThisComponent < 0)
                                      {
                                        ThisComponent += TexCoords.size();
                                      }
                                    else
                                      {
                                        ThisComponent -= 1;
                                      } /*if*/
                                    if (ThisComponent < 0 || ThisComponent >= TexCoords.size())
                                      {
                                        Parse.Fail("texcoord reference out of range");
                                      } /*if*/
                                    TexCoordIndex = ThisComponent;
                                break;
                                case 2:
                                    if (Normals == null)
                                      {
                                        Parse.Fail("normals referenced but not defined");
                                      } /*if*/
                                    if (ThisComponent < 0)
                                      {
                                        ThisComponent += Normals.size();
                                      }
                                    else
                                      {
                                        ThisComponent -= 1;
                                      } /*if*/
                                    if (ThisComponent < 0 || ThisComponent >= Normals.size())
                                      {
                                        Parse.Fail("normal reference out of range");
                                      } /*if*/
                                    NormalIndex = ThisComponent;
                                break;
                                  } /*switch*/
                              }
                            else
                              {
                                switch (Which)
                                  {
                                case 0:
                                    Parse.Fail("missing vertex reference");
                                break;
                                case 1:
                                    if (TexCoords != null)
                                      {
                                        Parse.Fail("missing texcoord reference");
                                      } /*if*/
                                break;
                                case 2:
                                    if (Normals != null)
                                      {
                                        Parse.Fail("missing normal reference");
                                      } /*if*/
                                break;
                                  }
                              } /*if*/
                            ++Which;
                            if (ThisPos == VertStr.length())
                                break;
                            if (Which == 3)
                              {
                                Parse.Fail("too many components for face vertex");
                              } /*if*/
                            ++ThisPos; /* skip slash */
                          } /*for*/
                        FaceVerts.add(new FaceVert(VertIndex, TexCoordIndex, NormalIndex));
                      } /*for*/
                    if (FaceVerts.size() < 3 || FaceVerts.size() > 4)
                      {
                        Parse.Fail("face must have 3 or 4 vertices");
                      } /*if*/
                    Faces.add(FaceVerts.toArray(new FaceVert[FaceVerts.size()]));
                  }
                else
                  {
                    System.err.printf
                      (
                        "ObjReader warning: ignoring op \"%s\" on line %d.\n",
                        Op,
                        Parse.LineNr
                      );
                    Parse.SkipRest();
                  } /*if*/
              } /*if*/
            Parse.EndLine();
            if (Parse.EOF)
                break;
          } /*for*/
        if (Faces == null)
          {
            throw new DataFormatException("ObjReader error: no faces defined");
          } /*if*/
        final GeomBuilder Geom = new GeomBuilder
          (
            /*GotNormals =*/ Normals != null,
            /*GotTexCoords =*/ TexCoords != null,
            /*GotColors =*/ false
          );
        final java.util.Map<FaceVert, Integer> FaceMap = new java.util.HashMap<FaceVert, Integer>();
        for (FaceVert[] Face : Faces)
          {
            for (FaceVert Vert : Face)
              {
                if (!FaceMap.containsKey(Vert))
                  {
                    FaceMap.put
                      (
                        Vert,
                        Geom.Add
                          (
                            /*Vertex =*/ Vertices.get(Vert.VertIndex),
                            /*Normal =*/
                                Vert.NormalIndex != null ?
                                    Normals.get(Vert.NormalIndex)
                                :
                                    null,
                            /*TexCoord =*/
                                Vert.TexCoordIndex != null ?
                                    TexCoords.get(Vert.TexCoordIndex)
                                :
                                    null,
                            /*VertexColor =*/ null
                          )
                      );
                  } /*if*/
              } /*for*/
            final int[] FaceVerts = new int[Face.length];
            for (int i = 0; i < Face.length; ++i)
              {
                FaceVerts[i] = FaceMap.get(Face[i]);
              } /*for*/
            switch (FaceVerts.length)
              {
            case 3:
                Geom.AddTri(FaceVerts[0], FaceVerts[1], FaceVerts[2]);
            break;
            case 4:
                Geom.AddQuad(FaceVerts[0], FaceVerts[1], FaceVerts[2], FaceVerts[3]);
            break;
              } /*switch*/
          } /*for*/
        return
            Geom.MakeObj();
      } /*Read*/

    public static GeomBuilder.Obj Read
      (
        String FileName
      )
    throws DataFormatException
      {
        GeomBuilder.Obj Result = null;
        java.io.FileInputStream ReadFrom = null;
        try
          {
            ReadFrom = new java.io.FileInputStream(FileName);
          }
        catch (java.io.IOException IOError)
          {
            throw new DataFormatException("I/O error: " + IOError.toString());
          } /*try*/
        if (ReadFrom != null)
          {
            Result = Read(ReadFrom);
            try
              {
                ReadFrom.close();
              }
            catch (java.io.IOException WhoCares)
              {
              /* I mean, really? */
              } /*try*/
          } /*if*/
        return
            Result;
      } /*Read*/

  } /*ObjReader*/
