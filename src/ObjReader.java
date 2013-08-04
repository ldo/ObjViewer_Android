package nz.gen.geek_central.GLUseful;
/*
    Basic reader for .obj 3D model files and associated .mtl material files.

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

import java.util.ArrayList;
import static nz.gen.geek_central.GLUseful.GLUseful.gl;

public class ObjReader
  {

    public static class DataFormatException extends RuntimeException
      /* indicates a problem parsing file contents. */
      {

        public DataFormatException
          (
            String Message
          )
          {
            super(Message);
          } /*DataFormatException*/

      } /*DataFormatException*/

    public static class Material
      {
        public final String Name;
      /* no support for texture files, functions or any of that stuff */
        public final GeomBuilder.Color
            AmbientColor, DiffuseColor, SpecularColor;
        public final float SpecularExponent;
        public final int Illum; /* NYI for now? */

        private static final float DefaultSpecularExponent = 100.0f /*?*/;
        private static final int DefaultIllum = 0 /*?*/;

        public Material
          (
            String Name
          )
          /* material with all default settings. */
          {
            this.Name = Name;
            this.AmbientColor = new GeomBuilder.Color(1.0f, 1.0f, 1.0f, 1.0f);
            this.DiffuseColor = new GeomBuilder.Color(1.0f, 1.0f, 1.0f, 1.0f);
            this.SpecularColor = new GeomBuilder.Color(1.0f, 1.0f, 1.0f, 1.0f);
            this.SpecularExponent = DefaultSpecularExponent;
            this.Illum = DefaultIllum;
          } /*Material*/

        public Material
          (
            String Name,
          /* all following parameters optional, alpha components ignored, taken from Dissolve */
            GeomBuilder.Color AmbientColor,
            GeomBuilder.Color DiffuseColor,
            GeomBuilder.Color SpecularColor,
            Float SpecularExponent,
            Float Dissolve,
            Integer Illum
          )
          {
            this.Name = Name;
            final float Alpha = Dissolve != null ? Dissolve : 1.0f;
            this.AmbientColor =
                AmbientColor != null ?
                    new GeomBuilder.Color
                      (
                        AmbientColor.r,
                        AmbientColor.g,
                        AmbientColor.b,
                        Alpha
                      )
                :
                    new GeomBuilder.Color(1.0f, 1.0f, 1.0f, Alpha);
            this.DiffuseColor =
                DiffuseColor != null ?
                    new GeomBuilder.Color
                      (
                        DiffuseColor.r,
                        DiffuseColor.g,
                        DiffuseColor.b,
                        Alpha
                      )
                :
                    new GeomBuilder.Color(1.0f, 1.0f, 1.0f, Alpha);
            this.SpecularColor =
                SpecularColor != null ?
                    new GeomBuilder.Color
                      (
                        SpecularColor.r,
                        SpecularColor.g,
                        SpecularColor.b,
                        Alpha
                      )
                :
                    new GeomBuilder.Color(1.0f, 1.0f, 1.0f, Alpha);
            this.SpecularExponent = SpecularExponent != null ? SpecularExponent : DefaultSpecularExponent;
            this.Illum = Illum != null ? Illum : DefaultIllum;
          } /*Material*/

        public void Apply()
          /* sets the material settings into the current GL context. */
          {
          /* Illum NYI */
            gl.glMaterialfv
              (
                /*face =*/ gl.GL_FRONT_AND_BACK,
                /*pname =*/ gl.GL_AMBIENT,
                /*params =*/
                    new float[]
                        {
                            AmbientColor.r,
                            AmbientColor.g,
                            AmbientColor.b,
                            AmbientColor.a,
                        },
                /*offset =*/ 0
              );
            gl.glMaterialfv
              (
                /*face =*/ gl.GL_FRONT_AND_BACK,
                /*pname =*/ gl.GL_DIFFUSE,
                /*params =*/
                    new float[]
                        {
                            DiffuseColor.r,
                            DiffuseColor.g,
                            DiffuseColor.b,
                            DiffuseColor.a,
                        },
                /*offset =*/ 0
              );
            gl.glMaterialfv
              (
                /*face =*/ gl.GL_FRONT_AND_BACK,
                /*pname =*/ gl.GL_SPECULAR,
                /*params =*/
                    new float[]
                        {
                            SpecularColor.r,
                            SpecularColor.g,
                            SpecularColor.b,
                            SpecularColor.a,
                        },
                /*offset =*/ 0
              );
            gl.glMaterialf
              (
                /*face =*/ gl.GL_FRONT_AND_BACK,
                /*pname =*/ gl.GL_SHININESS,
                /*param =*/ SpecularExponent
              );
          } /*Apply*/

      } /*Material*/

    public static class MaterialSet
      /* a mapping from material names to material definitions */
      {
        private final java.util.Map<String, Material> Materials =
            new java.util.HashMap<String, Material>();

        public MaterialSet()
          {
          } /*MaterialSet*/

        public void Add
          (
            Material TheMaterial
          )
          {
            Materials.put(TheMaterial.Name, TheMaterial);
          } /*Add*/

        public Material Get
          (
            String Name
          )
          {
            return
                Materials.get(Name);
          } /*Get*/

      } /*MaterialSet*/

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
                    GLUseful.StdLocale,
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
            /*final*/ char Result
                = (char)-1; /*sigh*/
            try
              {
                boolean LastWasBackslash = false;
                for (;;)
                  {
                    ++ColNr;
                    final int ich = Input.read();
                    if (ich < 0)
                      {
                        --ColNr;
                        EOF = true;
                        Result = '\n';
                        break;
                      }
                    else if (LastWasCR && (char)ich == '\012')
                      {
                      /* skip LF following CR */
                        --ColNr;
                        LastWasCR = false;
                        LastWasBackslash = false;
                      }
                    else if (!LastWasBackslash && (char)ich == '\\')
                      {
                        LastWasBackslash = true;
                        LastWasCR = false;
                      }
                    else if (!LastWasBackslash || !IsEOL((char)ich))
                      {
                        Result = (char)ich;
                        break;
                      }
                    else
                      {
                        ++LineNr;
                        ColNr = 0;
                        LastWasCR = (char)ich == '\015';
                        LastWasBackslash = false;
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

        public String GetRestOfLine()
          /* returns rest of current line (excluding leading whitespace, but
            including embedded whitespace) as a symbol. There must be at least
            one non-whitespace character. */
          {
            final StringBuilder CurSym = new StringBuilder();
            for (;;)
              {
                if (EOL)
                    break;
                final char Ch = NextCh();
                if (!InComment)
                  {
                    if (IsEOL(Ch))
                      {
                        break;
                      }
                    else if (IsSeparator(Ch) && CurSym.length() == 0)
                      {
                      /* ignore leading whitespace */
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
            if (Result == null)
              {
                Fail("missing required symbol");
              } /*if*/
            return
                Result;
          } /*GetRestOfLine*/

        public float GetFloat
          (
            String Description
          )
          {
            float Result = 0.0f;
            try
              {
                Result = Float.parseFloat(NextSym(true));
              }
            catch (NumberFormatException BadNum)
              {
                Fail("bad " + Description);
              } /*try*/
            return
                Result;
          } /*GetFloat*/

        public Vec3f GetVec
          (
            int MinD, /* [1 .. 3] */
            int MaxD /* [3 .. 4] */
          )
          {
            final String XStr = NextSym(true);
            final String YStr = NextSym(MinD >= 2);
            final String ZStr = NextSym(MinD >= 3);
            final String WStr =
                MaxD == 4 ?
                    NextSym(MinD == 4)
                :
                    null;
            /*final*/ Vec3f Result
                = null; /*sigh*/
            try
              {
                final float Y =
                    YStr != null ?
                        Float.parseFloat(YStr)
                    :
                        1.0f;
                final float Z =
                    ZStr != null ?
                        Float.parseFloat(ZStr)
                    :
                        1.0f;
                final float W =
                    WStr != null ?
                        Float.parseFloat(WStr)
                    :
                        1.0f;
                Result = new Vec3f
                  (
                    Float.parseFloat(XStr) / W,
                    Y / W,
                    Z / W
                  );
              }
            catch (NumberFormatException BadNum)
              {
                Fail("bad vector coordinate");
              } /*try*/
            return
                Result;
          } /*GetVec*/

        public GeomBuilder.Color GetColor()
          {
          /* colours in .obj don't have individual alpha */
            final String RStr = NextSym(true);
            final String GStr = NextSym(true);
            final String BStr = NextSym(true);
            /*final*/ GeomBuilder.Color Result
                = null; /*sigh*/
            try
              {
                Result = new GeomBuilder.Color
                  (
                    Float.parseFloat(RStr),
                    Float.parseFloat(GStr),
                    Float.parseFloat(BStr),
                    1.0f
                  );
              }
            catch (NumberFormatException BadNum)
              {
                Fail("bad RGB colour component");
              } /*try*/
            return
                Result;
          } /*GetColor*/

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

    public interface MaterialLoader
      {

        public MaterialSet Load
          (
            MaterialSet PrevMaterials, /* can simply add to this or replace it */
            String FileName
          );

      } /*MaterialLoader*/

    public static class Model
      {
        public final Vec3f BoundMin, BoundMax;

        private static class Component
          {
            private final Material ObjMaterial; /* optional */
            private final GeomBuilder.Obj ObjGeometry;

            private Component
              (
                Material ObjMaterial,
                GeomBuilder.Obj ObjGeometry
              )
              {
                this.ObjMaterial = ObjMaterial;
                this.ObjGeometry = ObjGeometry;
              } /*Component*/
          } /*Component*/

        private final Component[] Components;

        private Model
          (
            Component[] Components,
            Vec3f BoundMin,
            Vec3f BoundMax
          )
          {
            this.Components = Components;
            this.BoundMin = BoundMin;
            this.BoundMax = BoundMax;
          } /*Model*/

        public void Draw()
          {
            for (Component ThisComponent : Components)
              {
                if (ThisComponent.ObjMaterial != null)
                  {
                    ThisComponent.ObjMaterial.Apply();
                  } /*if*/
                ThisComponent.ObjGeometry.Draw();
              } /*for*/
          } /*Draw*/

      } /*Model*/

    public static Model ReadObj
      (
        java.io.InputStream From,
        MaterialLoader LoadMaterials
      )
    throws DataFormatException
      {
        final ObjTokenizer Parse = new ObjTokenizer(From);
        ArrayList<Vec3f>
            Vertices = null,
            TexCoords = null,
            Normals = null;
        ArrayList<FaceVert[]> Faces = null;
        MaterialSet LoadedMaterials = new MaterialSet();
        final ArrayList<Model.Component> ModelComponents = new ArrayList<Model.Component>();
        Material CurMaterial = new Material("");
        Vec3f
            BoundMin = null,
            BoundMax = null;
        for (;;)
          {
            String Op = Parse.NextSym(false);
            if (Op != null)
              {
                Op = Op.intern();
              } /*if*/
            if (Op == null && Parse.EOF || Op == "usemtl")
              {
                if (Faces != null)
                  {
                  /* finish last object */
                    final GeomBuilder Geom = new GeomBuilder
                      (
                        /*GotNormals =*/ Normals != null,
                        /*GotTexCoords =*/ TexCoords != null,
                        /*GotColors =*/ false
                      );
                    final java.util.Map<FaceVert, Integer> FaceMap =
                        new java.util.HashMap<FaceVert, Integer>();
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
                        Geom.AddPoly(FaceVerts);
                      } /*for*/
                    final GeomBuilder.Obj NewObj = Geom.MakeObj();
                    if (BoundMin != null)
                      {
                        BoundMin =
                            new Vec3f
                              (
                                Math.min(BoundMin.x, NewObj.BoundMin.x),
                                Math.min(BoundMin.y, NewObj.BoundMin.y),
                                Math.min(BoundMin.z, NewObj.BoundMin.z)
                              );
                      }
                    else
                      {
                        BoundMin = NewObj.BoundMin;
                      } /*if*/
                    if (BoundMax != null)
                      {
                        BoundMax =
                            new Vec3f
                              (
                                Math.max(BoundMax.x, NewObj.BoundMax.x),
                                Math.max(BoundMax.y, NewObj.BoundMax.y),
                                Math.max(BoundMax.z, NewObj.BoundMax.z)
                              );
                      }
                    else
                      {
                        BoundMax = NewObj.BoundMax;
                      } /*if*/
                    ModelComponents.add
                      (
                        new Model.Component(CurMaterial, NewObj)
                      );
                    Faces = null;
                  } /*if*/
                if (Op != null)
                  {
                    CurMaterial = LoadedMaterials.Get(Parse.NextSym(true));
                  } /*if*/
              }
            else if (Op != null)
              {
                if (Op == "v")
                  {
                    final Vec3f Vec = Parse.GetVec(3, 4);
                    if (Vertices == null)
                      {
                        Vertices = new ArrayList<Vec3f>();
                      } /*if*/
                    Vertices.add(Vec);
                  }
                else if (Op == "vt")
                  {
                    final Vec3f Vec = Parse.GetVec(1, 3);
                    if (TexCoords == null)
                      {
                        TexCoords = new ArrayList<Vec3f>();
                      } /*if*/
                    TexCoords.add(Vec);
                  }
                else if (Op == "vn")
                  {
                    final Vec3f Vec = Parse.GetVec(3, 3);
                    if (Normals == null)
                      {
                        Normals = new ArrayList<Vec3f>();
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
                                    Parse.Fail(String.format(GLUseful.StdLocale, "bad vertex index \"%s\"", VertStr.substring(LastPos, ThisPos)));
                                  } /*try*/
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
                        if (VertIndex == null)
                          {
                            Parse.Fail("missing vertex reference");
                          } /*if*/
                        if (TexCoordIndex == null && TexCoords != null)
                          {
                            Parse.Fail("missing texcoord reference");
                          } /*if*/
                        if (NormalIndex == null && Normals != null)
                          {
                            Parse.Fail("missing normal reference");
                          } /*if*/
                        FaceVerts.add(new FaceVert(VertIndex, TexCoordIndex, NormalIndex));
                      } /*for*/
                    Faces.add(FaceVerts.toArray(new FaceVert[FaceVerts.size()]));
                  }
                else if (Op == "mtllib")
                  {
                  /* Blender only expects a single library file name, which might have embedded spaces */
                    LoadedMaterials = LoadMaterials.Load(LoadedMaterials, Parse.GetRestOfLine());
                    if (LoadedMaterials == null)
                      {
                        Parse.Fail
                          (
                            "materials loader didn't return anything"
                          );
                      } /*if*/
                  }
                else
                  {
                    System.err.printf
                      (
                        "ObjReader.ReadObj warning: ignoring op \"%s\" on line %d.\n",
                        Op,
                        Parse.LineNr
                      );
                    Parse.SkipRest();
                  } /*if*/
              } /*if*/
            Parse.EndLine();
            if (Parse.EOF && Faces == null)
                break;
          } /*for*/
        return
            new Model
              (
                ModelComponents.toArray(new Model.Component[ModelComponents.size()]),
                BoundMin,
                BoundMax
              );
      } /*ReadObj*/

    public static MaterialSet ReadMaterials
      (
        java.io.InputStream From,
        MaterialSet CurMaterials
      )
      {
        final ObjTokenizer Parse = new ObjTokenizer(From);
        String MaterialName = null;
        GeomBuilder.Color
            AmbientColor = null,
            DiffuseColor = null,
            SpecularColor = null;
        Float
            SpecularExponent = null,
            Dissolve = null;
        Integer
            Illum = null;
        for (;;)
          {
            String Op = Parse.NextSym(false);
            if (Op != null)
              {
                Op = Op.intern();
              } /*if*/
            if (Op == null && Parse.EOF || Op == "newmtl")
              {
                if (MaterialName != null)
                  {
                    if (!CurMaterials.Materials.containsKey(MaterialName)) /* let earlier entry take precedence */
                      {
                        CurMaterials.Materials.put
                          (
                            MaterialName,
                            new Material
                              (
                                /*Name =*/ MaterialName,
                                /*AmbientColor =*/ AmbientColor,
                                /*DiffuseColor =*/ DiffuseColor,
                                /*SpecularColor =*/ SpecularColor,
                                /*SpecularExponent =*/ SpecularExponent,
                                /*Dissolve =*/ Dissolve,
                                /*Illum =*/ Illum
                              )
                          );
                      } /*if*/
                    MaterialName = null;
                  } /*if*/
                if (Op != null)
                  {
                    MaterialName = Parse.NextSym(true).intern();
                    AmbientColor = null;
                    DiffuseColor = null;
                    SpecularColor = null;
                    SpecularExponent = null;
                    Dissolve = null;
                    Illum = null;
                  } /*if*/
              }
            else if (Op != null)
              {
                if (MaterialName == null)
                  {
                    Parse.Fail("material definition doesn’t begin with “newmtl”");
                  } /*if*/
                if (Op == "Ka")
                  {
                    AmbientColor = Parse.GetColor();
                  }
                else if (Op == "Kd")
                  {
                    DiffuseColor = Parse.GetColor();
                  }
                else if (Op == "Ks")
                  {
                    SpecularColor = Parse.GetColor();
                  }
                else if (Op == "Ns")
                  {
                    SpecularExponent = Parse.GetFloat("specular exponent");
                  }
                else if (Op == "d")
                  {
                    Dissolve = Parse.GetFloat("dissolve");
                  }
              /* "illum" NYI */
                else
                  {
                    System.err.printf
                      (
                        "ObjReader.ReadMaterials warning: ignoring op \"%s\" on line %d.\n",
                        Op,
                        Parse.LineNr
                      );
                    Parse.SkipRest();
                  } /*if*/
              } /*if*/
            Parse.EndLine();
            if (Parse.EOF && MaterialName == null)
                break;
          } /*for*/
        return
            CurMaterials;
      } /*ReadMaterials*/

    public static Model ReadObj
      (
        String FileName,
        MaterialLoader LoadMaterials
      )
    throws DataFormatException
      {
        Model Result = null;
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
            Result = ReadObj(ReadFrom, LoadMaterials);
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
      } /*ReadObj*/

    public static MaterialSet ReadMaterials
      (
        String FileName,
        MaterialSet CurMaterials
      )
      {
        MaterialSet Result = null;
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
            Result = ReadMaterials(ReadFrom, CurMaterials);
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
      } /*ReadMaterials*/

  } /*ObjReader*/
