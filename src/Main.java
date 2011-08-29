package nz.gen.geek_central.ObjViewer;

import nz.gen.geek_central.GLUseful.ObjReader;

public class Main extends android.app.Activity
  {
    java.util.Map<android.view.MenuItem, Runnable> OptionsMenu;

    ObjectView TheObjectView;

  /* request codes, all arbitrarily assigned */
    static final int LoadObjectRequest = 1;

    interface RequestResponseAction /* response to an activity result */
      {
        public void Run
          (
            int ResultCode,
            android.content.Intent Data
          );
      } /*RequestResponseAction*/

    java.util.Map<Integer, RequestResponseAction> ActivityResultActions;

    @Override
    public boolean onCreateOptionsMenu
      (
        android.view.Menu TheMenu
      )
      {
        OptionsMenu = new java.util.HashMap<android.view.MenuItem, Runnable>();
        OptionsMenu.put
          (
            TheMenu.add(R.string.pick_file),
            new Runnable()
              {
                public void run()
                  {
                    Picker.Launch
                      (
                        /*Acting =*/ Main.this,
                        /*RequestCode =*/ LoadObjectRequest,
                        /*LookIn =*/
                            new String[]
                                {
                                    "Models",
                                    "Download",
                                }
                      );
                  } /*run*/
              } /*Runnable*/
          );
        return
            true;
      } /*onCreateOptionsMenu*/

    void BuildActivityResultActions()
      {
        ActivityResultActions = new java.util.HashMap<Integer, RequestResponseAction>();
        ActivityResultActions.put
          (
            LoadObjectRequest,
            new RequestResponseAction()
              {
                public void Run
                  (
                    int ResultCode,
                    android.content.Intent Data
                  )
                  {
                    final String FileName = Data.getData().getPath();
                    nz.gen.geek_central.GLUseful.GeomBuilder.Obj NewObj = null;
                    try
                      {
                        NewObj = ObjReader.Read(FileName);
                      }
                    catch (ObjReader.DataFormatException Failed)
                      {
                        android.widget.Toast.makeText
                          (
                            /*context =*/ Main.this,
                            /*text =*/
                                String.format
                                  (
                                    getString(R.string.obj_load_fail),
                                    Failed.toString()
                                  ),
                            /*duration =*/ android.widget.Toast.LENGTH_SHORT
                          ).show();
                      } /*try*/
                    if (NewObj != null)
                      {
                        TheObjectView.SetObject(NewObj);
                      } /*if*/
                  } /*Run*/
              } /*RequestResponseAction*/
          );
      } /*BuildActivityResultActions*/

    @Override
    public void onCreate
      (
        android.os.Bundle SavedInstanceState
      )
      {
        super.onCreate(SavedInstanceState);
        setContentView(R.layout.main);
        TheObjectView = (ObjectView)findViewById(R.id.object_view);
        BuildActivityResultActions();
      } /*onCreate*/

    @Override
    public boolean onOptionsItemSelected
      (
        android.view.MenuItem TheItem
      )
      {
        boolean Handled = false;
        final Runnable Action = OptionsMenu.get(TheItem);
        if (Action != null)
          {
            Action.run();
            Handled = true;
          } /*if*/
        return
            Handled;
      } /*onOptionsItemSelected*/

    @Override
    public void onActivityResult
      (
        int RequestCode,
        int ResultCode,
        android.content.Intent Data
      )
      {
        Picker.Cleanup();
        if (ResultCode != android.app.Activity.RESULT_CANCELED)
          {
            final RequestResponseAction Action = ActivityResultActions.get(RequestCode);
            if (Action != null)
              {
                Action.Run(ResultCode, Data);
              } /*if*/
          } /*if*/
      } /*onActivityResult*/

  } /*Main*/
