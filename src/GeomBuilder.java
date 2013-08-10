package nz.gen.geek_central.GLUseful;
/*
    Easy construction and application of buffers needed for
    OpenGL-ES drawing.

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
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import static nz.gen.geek_central.GLUseful.GLUseful.gl;

public class GeomBuilder
  /*
    Helper class for easier construction of geometrical
    objects. Instantiate this and tell it whether each vertex will
    also have a normal vector, a texture-coordinate vector or a
    colour. Then call Add to add vertex definitions (using class Vec3f
    to define points, and GeomBuilder.Color to define colours), and
    use the returned vertex indices to construct faces with AddTri and
    AddQuad. Finally, call MakeObj to obtain a GeomBuilder.Obj that
    has a Draw method that will render the resulting geometry into a
    specified GL context.
  */
  {

    public static class Color
      /* RGB colours with transparency */
      {
        public final float r, g, b, a;

        public Color
          (
            float r,
            float g,
            float b,
            float a
          )
          {
            this.r = r;
            this.b = b;
            this.g = g;
            this.a = a;
          } /*Color*/

      } /*Color*/

    private final boolean AutoNormals;
    private final ArrayList<Vec3f> TempPoints, TempPointTexCoords;
    private final ArrayList<Color> TempPointColors;
    private final ArrayList<Vec3f> Points, PointNormals, PointTexCoords;
    private final ArrayList<Color> PointColors;
    private final ArrayList<Integer> Faces;
    private Vec3f BoundMin, BoundMax;

    public GeomBuilder
      (
        boolean GotNormals, /* vertices will have normals specified, otherwise they will be automatically generated for flat shading */
        boolean GotTexCoords, /* vertices will have texture coordinates specified */
        boolean GotColors /* vertices will have colours specified */
      )
      {
        AutoNormals = !GotNormals;
        TempPoints = AutoNormals ? new ArrayList<Vec3f>() : null;
        Points = new ArrayList<Vec3f>();
        PointNormals = GotNormals || AutoNormals ? new ArrayList<Vec3f>() :  null;
        TempPointTexCoords = AutoNormals && GotTexCoords ? new ArrayList<Vec3f>() : null;
        PointTexCoords = GotTexCoords ? new ArrayList<Vec3f>() : null;
        TempPointColors = AutoNormals && GotColors ? new ArrayList<Color>() : null;
        PointColors = GotColors ? new ArrayList<Color>() : null;
        Faces = new ArrayList<Integer>();
        BoundMin = null;
        BoundMax = null;
      } /*GeomBuilder*/

    private int Add
      (
        Vec3f Vertex,
      /* following args are either mandatory or must be null, depending
        on respective flags passed to constructor */
        Vec3f Normal,
        Vec3f TexCoord,
        Color VertexColor,
        boolean AutoNormals
      )
      /* adds a new vertex, and returns its index for use in constructing faces. */
      {
        if
          (
                AutoNormals != (Normal == null)
            ||
                (PointColors == null) != (VertexColor == null)
            ||
                (PointTexCoords == null) != (TexCoord == null)
          )
          {
            throw new RuntimeException("missing or redundant args specified");
          } /*if*/
        final int Result = AutoNormals ? TempPoints.size() : Points.size();
        (AutoNormals ? TempPoints : Points).add(Vertex);
        if (!AutoNormals)
          {
            PointNormals.add(Normal);
          } /*if*/
        if (PointTexCoords != null)
          {
            (AutoNormals ? TempPointTexCoords : PointTexCoords).add(TexCoord);
          } /*if*/
        if (PointColors != null)
          {
            (AutoNormals ? TempPointColors : PointColors).add(VertexColor);
          } /*if*/
        if (AutoNormals == this.AutoNormals)
          {
            if (BoundMin != null)
              {
                BoundMin =
                    new Vec3f
                      (
                        Math.min(BoundMin.x, Vertex.x),
                        Math.min(BoundMin.y, Vertex.y),
                        Math.min(BoundMin.z, Vertex.z)
                      );
              }
            else
              {
                BoundMin = Vertex;
              } /*if*/
            if (BoundMax != null)
              {
                BoundMax =
                    new Vec3f
                      (
                        Math.max(BoundMax.x, Vertex.x),
                        Math.max(BoundMax.y, Vertex.y),
                        Math.max(BoundMax.z, Vertex.z)
                      );
              }
            else
              {
                BoundMax = Vertex;
              } /*if*/
          } /*if*/
        return
            Result;
      } /*Add*/

    public int Add
      (
        Vec3f Vertex,
      /* following args are either mandatory or must be null, depending
        on respective flags passed to constructor */
        Vec3f Normal,
        Vec3f TexCoord,
        Color VertexColor
      )
      /* adds a new vertex, and returns its index for use in constructing faces. */
      {
        return
            Add
              (
                /*Vertex =*/ Vertex,
                /*Normal =*/ Normal,
                /*TexCoord =*/ TexCoord,
                /*VertexColor =*/ VertexColor,
                /*AutoNormals =*/ AutoNormals
              );
      } /*Add*/

    private int AddActual
      (
        int VertIndex,
        Vec3f Normal
      )
      {
        if (!AutoNormals)
          {
            throw new RuntimeException("GeomBuilder.AddActual shouldn’t be called if not AutoNormals");
          } /*if*/
        return
            Add
              (
                /*Vertex =*/ TempPoints.get(VertIndex),
                /*Normal =*/ Normal,
                /*TexCoord =*/ TempPointTexCoords != null ? TempPointTexCoords.get(VertIndex) : null,
                /*VertexColor =*/ TempPointColors != null ? TempPointColors.get(VertIndex) : null,
                /*AutoNormals =*/ false
              );
      } /*AddActual*/

    private void GenAutoNormal
      (
        int[] Vertices
          /* assume length at least 3 and coplanar if > 3, replaced with final generated vertices */
      )
      {
        if (!AutoNormals)
          {
            throw new RuntimeException("GeomBuilder.GenAutoNormal shouldn’t be called if not AutoNormals");
          } /*if*/
        final Vec3f
            V1 = TempPoints.get(Vertices[0]),
            V2 = TempPoints.get(Vertices[1]),
            V3 = TempPoints.get(Vertices[2]);
        final Vec3f FaceNormal = (V2.sub(V1)).cross(V3.sub(V2)).unit();
        final int[] NewVertices = new int[Vertices.length];
        for (int i = 0; i < Vertices.length; ++i)
          {
            NewVertices[i] = AddActual(Vertices[i], FaceNormal);
          } /*for*/
        System.arraycopy(NewVertices, 0, Vertices, 0, Vertices.length);
      } /*GenAutoNormal*/

    private void AddTri
      (
        int V1,
        int V2,
        int V3,
        boolean AutoNormals
      )
      /* defines a triangular face. Args are indices as previously returned from calls to Add. */
      {
        if (AutoNormals)
          {
            final int[] Vertices = new int[] {V1, V2, V3};
            GenAutoNormal(Vertices);
            V1 = Vertices[0];
            V2 = Vertices[1];
            V3 = Vertices[2];
          } /*if*/
        Faces.add(V1);
        Faces.add(V2);
        Faces.add(V3);
      } /*AddTri*/

    public void AddTri
      (
        int V1,
        int V2,
        int V3
      )
      /* defines a triangular face. Args are indices as previously returned from calls to Add. */
      {
        AddTri(V1, V2, V3, AutoNormals);
      } /*AddTri*/

    public void AddQuad
      (
        int V1,
        int V2,
        int V3,
        int V4
      )
      /* Defines a quadrilateral face. Args are indices as previously returned from calls to Add. */
      {
        if (AutoNormals)
          {
            final int[] Vertices = new int[] {V1, V2, V3, V4};
            GenAutoNormal(Vertices);
            V1 = Vertices[0];
            V2 = Vertices[1];
            V3 = Vertices[2];
            V4 = Vertices[3];
          } /*if*/
        AddTri(V1, V2, V3, false);
        AddTri(V4, V1, V3, false);
      } /*AddQuad*/

    public void AddPoly
      (
        int[] V
      )
      /* Defines a polygonal face. Array elements are indices as previously
        returned from calls to Add. */
      {
        if (AutoNormals)
          {
            final int[] V2 = new int[V.length];
            System.arraycopy(V, 0, V2, 0, V.length);
            GenAutoNormal(V2);
            V = V2;
          } /*if*/
        for (int i = 1; i < V.length - 1; ++i)
          {
            AddTri(V[0], V[i], V[i + 1], false);
          } /*for*/
      } /*AddPoly*/

    public static class Obj
      /* representation of complete object geometry. */
      {
        private final IntBuffer VertexBuffer;
        private final IntBuffer NormalBuffer;
        private final IntBuffer TexCoordBuffer;
        private final IntBuffer ColorBuffer;
        private final ShortBuffer IndexBuffer;
        private final int NrIndexes;
        public final Vec3f BoundMin, BoundMax;

        private Obj
          (
            IntBuffer VertexBuffer,
            IntBuffer NormalBuffer, /* optional */
            IntBuffer TexCoordBuffer, /* optional */
            IntBuffer ColorBuffer, /* optional */
            ShortBuffer IndexBuffer,
            int NrIndexes,
            Vec3f BoundMin,
            Vec3f BoundMax
          )
          {
            this.VertexBuffer = VertexBuffer;
            this.NormalBuffer = NormalBuffer;
            this.TexCoordBuffer = TexCoordBuffer;
            this.ColorBuffer = ColorBuffer;
            this.IndexBuffer = IndexBuffer;
            this.NrIndexes = NrIndexes;
            this.BoundMin = BoundMin;
            this.BoundMax = BoundMax;
          } /*Obj*/

        public void Draw()
          /* actually renders the geometry into the specified GL context. */
          {
            gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, gl.GL_FIXED, 0, VertexBuffer);
            if (NormalBuffer != null)
              {
                gl.glEnableClientState(gl.GL_NORMAL_ARRAY);
                gl.glNormalPointer(gl.GL_FIXED, 0, NormalBuffer);
              } /*if*/
            if (TexCoordBuffer != null)
              {
                gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
                gl.glTexCoordPointer(3, gl.GL_FIXED, 0, TexCoordBuffer);
              } /*if*/
            if (ColorBuffer != null)
              {
                gl.glEnableClientState(gl.GL_COLOR_ARRAY);
                gl.glColorPointer(4, gl.GL_FIXED, 0, ColorBuffer);
              } /*if*/
            gl.glDrawElements(gl.GL_TRIANGLES, NrIndexes, gl.GL_UNSIGNED_SHORT, IndexBuffer);
            gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
            gl.glDisableClientState(gl.GL_NORMAL_ARRAY);
            gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
            gl.glDisableClientState(gl.GL_COLOR_ARRAY);
          } /*Draw*/

      } /*Obj*/;

    public Obj MakeObj()
      /* constructs and returns the final geometry ready for rendering. */
      {
        if (Points.size() == 0)
          {
            throw new RuntimeException("GeomBuilder: empty object");
          } /*if*/
        final int Fixed1 = 0x10000;
        final int[] Vertices = new int[Points.size() * 3];
        final int[] Normals = PointNormals != null ? new int[Points.size() * 3] : null;
        final int[] TexCoords = PointTexCoords != null ? new int[Points.size() * 3] : null;
        final int[] Colors = PointColors != null ? new int[Points.size() * 4] : null;
        int jv = 0, jn = 0, jt = 0, jc = 0;
        for (int i = 0; i < Points.size(); ++i)
          {
            final Vec3f Point = Points.get(i);
            Vertices[jv++] = (int)(Point.x * Fixed1);
            Vertices[jv++] = (int)(Point.y * Fixed1);
            Vertices[jv++] = (int)(Point.z * Fixed1);
            if (PointNormals != null)
              {
                final Vec3f PointNormal = PointNormals.get(i);
                Normals[jn++] = (int)(PointNormal.x * Fixed1);
                Normals[jn++] = (int)(PointNormal.y * Fixed1);
                Normals[jn++] = (int)(PointNormal.z * Fixed1);
              } /*if*/
            if (PointTexCoords != null)
              {
                final Vec3f Coord = PointTexCoords.get(i);
                TexCoords[jt++] = (int)(Coord.x * Fixed1);
                TexCoords[jt++] = (int)(Coord.y * Fixed1);
                TexCoords[jt++] = (int)(Coord.z * Fixed1);
              } /*if*/
            if (PointColors != null)
              {
                final Color ThisColor = PointColors.get(i);
                Colors[jc++] = (int)(ThisColor.r * Fixed1);
                Colors[jc++] = (int)(ThisColor.g * Fixed1);
                Colors[jc++] = (int)(ThisColor.b * Fixed1);
                Colors[jc++] = (int)(ThisColor.a * Fixed1);
              } /*if*/
          } /*for*/
        final short[] Indices = new short[Faces.size()];
        final int NrIndexes = Indices.length;
        for (int i = 0; i < NrIndexes; ++i)
          {
            Indices[i] = (short)(int)Faces.get(i);
          } /*for*/
      /* Need to use allocateDirect to allocate buffers so garbage
        collector won't move them. Also make sure byte order is
        always native. But direct-allocation and order-setting methods
        are only available for ByteBuffer. Which is why buffers
        are allocated as ByteBuffers and then converted to more
        appropriate types. */
        final IntBuffer VertexBuffer;
        final IntBuffer NormalBuffer;
        final IntBuffer TexCoordBuffer;
        final IntBuffer ColorBuffer;
        final ShortBuffer IndexBuffer;
        VertexBuffer =
            ByteBuffer.allocateDirect(Vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(Vertices);
        VertexBuffer.position(0);
        if (PointNormals != null)
          {
            NormalBuffer =
                ByteBuffer.allocateDirect(Normals.length * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer()
                .put(Normals);
            NormalBuffer.position(0);
          }
        else
          {
            NormalBuffer = null;
          } /*if*/
        if (PointTexCoords != null)
          {
            TexCoordBuffer =
                ByteBuffer.allocateDirect(TexCoords.length * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer()
                .put(TexCoords);
            TexCoordBuffer.position(0);
          }
        else
          {
            TexCoordBuffer = null;
          } /*if*/
        if (PointColors != null)
          {
            ColorBuffer =
                ByteBuffer.allocateDirect(Colors.length * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer()
                .put(Colors);
            ColorBuffer.position(0);
          }
        else
          {
            ColorBuffer = null;
          } /*if*/
        IndexBuffer =
            ByteBuffer.allocateDirect(Indices.length * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(Indices);
        IndexBuffer.position(0);
        return
            new Obj
              (
                VertexBuffer,
                NormalBuffer,
                TexCoordBuffer,
                ColorBuffer,
                IndexBuffer,
                NrIndexes,
                BoundMin,
                BoundMax
              );
      } /*MakeObj*/

  } /*GeomBuilder*/;
