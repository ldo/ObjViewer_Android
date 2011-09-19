package nz.gen.geek_central.GLUseful;
/*
    Easy construction and application of buffers needed for
    OpenGL-ES drawing.

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
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import android.opengl.GLES11;

public class GeomBuilder
  /*
    Helper class for easier construction of geometrical
    objects. Instantiate this and tell it whether each vertex will
    also have a normal vector, a texture-coordinate vector or a
    colour. Then call Add to add vertex definitions (using class
    GeomBuilder.Vec3f to define points, and GeomBuilder.Color to
    define colours), and use the returned vertex indices to construct
    faces with AddTri and AddQuad. Finally, call MakeObj to obtain a
    GeomBuilder.Obj that has a Draw method that will render the
    resulting geometry into a specified GL context.
  */
  {

    public static class Vec3f
      /* 3D vectors/points */
      {
        public final float x, y, z;

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
          } /*Vec3f*/

      } /*Vec3f*/

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

    private final ArrayList<Vec3f> Points;
    private final ArrayList<Vec3f> PointNormals;
    private final ArrayList<Vec3f> PointTexCoords;
    private final ArrayList<Color> PointColors;
    private final ArrayList<Integer> Faces;
    private Vec3f BoundMin, BoundMax;

    public GeomBuilder
      (
        boolean GotNormals, /* vertices will have normals specified */
        boolean GotTexCoords, /* vertices will have texture coordinates specified */
        boolean GotColors /* vertices will have colours specified */
      )
      {
        Points = new ArrayList<Vec3f>();
        PointNormals = GotNormals ? new ArrayList<Vec3f>() : null;
        PointTexCoords = GotTexCoords ? new ArrayList<Vec3f>() : null;
        PointColors = GotColors ? new ArrayList<Color>() : null;
        Faces = new ArrayList<Integer>();
        BoundMin = null;
        BoundMax = null;
      } /*GeomBuilder*/

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
        if
          (
                (PointNormals == null) != (Normal == null)
            ||
                (PointColors == null) != (VertexColor == null)
            ||
                (PointTexCoords == null) != (TexCoord == null)
          )
          {
            throw new RuntimeException("missing or redundant args specified");
          } /*if*/
        final int Result = Points.size();
        Points.add(Vertex);
        if (PointNormals != null)
          {
            PointNormals.add(Normal);
          } /*if*/
        if (PointTexCoords != null)
          {
            PointTexCoords.add(TexCoord);
          } /*if*/
        if (PointColors != null)
          {
            PointColors.add(VertexColor);
          } /*if*/
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
        return
            Result;
      } /*Add*/

    public void AddTri
      (
        int V1,
        int V2,
        int V3
      )
      /* defines a triangular face. Args are indices as previously returned from calls to Add. */
      {
        Faces.add(V1);
        Faces.add(V2);
        Faces.add(V3);
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
        AddTri(V1, V2, V3);
        AddTri(V4, V1, V3);
      } /*AddQuad*/

    public void AddPoly
      (
        int[] V
      )
      /* Defines a polygonal face. Array elements are indices as previously
        returned from calls to Add. */
      {
        for (int i = 1; i < V.length - 1; ++i)
          {
            AddTri(V[0], V[i], V[i + 1]);
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
            GLES11.glEnableClientState(GLES11.GL_VERTEX_ARRAY);
            GLES11.glVertexPointer(3, GLES11.GL_FIXED, 0, VertexBuffer);
            if (NormalBuffer != null)
              {
                GLES11.glEnableClientState(GLES11.GL_NORMAL_ARRAY);
                GLES11.glNormalPointer(GLES11.GL_FIXED, 0, NormalBuffer);
              } /*if*/
            if (TexCoordBuffer != null)
              {
                GLES11.glEnableClientState(GLES11.GL_TEXTURE_COORD_ARRAY);
                GLES11.glTexCoordPointer(3, GLES11.GL_FIXED, 0, TexCoordBuffer);
              } /*if*/
            if (ColorBuffer != null)
              {
                GLES11.glEnableClientState(GLES11.GL_COLOR_ARRAY);
                GLES11.glColorPointer(4, GLES11.GL_FIXED, 0, ColorBuffer);
              } /*if*/
            GLES11.glDrawElements(GLES11.GL_TRIANGLES, NrIndexes, GLES11.GL_UNSIGNED_SHORT, IndexBuffer);
            GLES11.glDisableClientState(GLES11.GL_VERTEX_ARRAY);
            GLES11.glDisableClientState(GLES11.GL_NORMAL_ARRAY);
            GLES11.glDisableClientState(GLES11.GL_TEXTURE_COORD_ARRAY);
            GLES11.glDisableClientState(GLES11.GL_COLOR_ARRAY);
          } /*Draw*/

      } /*Obj*/

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

  } /*GeomBuilder*/
