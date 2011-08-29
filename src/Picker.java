package nz.gen.geek_central.ObjViewer;
/*
    let the user choose an obj file to load
*/

public class Picker extends android.app.Activity
  {
    static boolean Reentered = false; /* sanity check */
    public static Picker Current = null;

    static String[] LookIn;

    android.widget.ListView PickerListView;
    SelectedItemAdapter PickerList;

    public static class PickerItem
      {
        String FullPath;
        boolean Selected;

        public PickerItem
          (
            String FullPath
          )
          {
            this.FullPath = FullPath;
            this.Selected = false;
          } /*PickerItem*/

        public String toString()
          /* returns the display name for the item. I use
            the unqualified filename. */
          {
            return
                new java.io.File(FullPath).getName();
          } /*toString*/

      } /*PickerItem*/

    class SelectedItemAdapter extends android.widget.ArrayAdapter<PickerItem>
      {
        final int ResID;
        final android.view.LayoutInflater TemplateInflater;
        PickerItem CurSelected;
        android.widget.RadioButton LastChecked;

        class OnSetCheck implements android.view.View.OnClickListener
          {
            final PickerItem MyItem;

            public OnSetCheck
              (
                PickerItem TheItem
              )
              {
                MyItem = TheItem;
              } /*OnSetCheck*/

            public void onClick
              (
                android.view.View TheView
              )
              {
                if (MyItem != CurSelected)
                  {
                  /* only allow one item to be selected at a time */
                    if (CurSelected != null)
                      {
                        CurSelected.Selected = false;
                        LastChecked.setChecked(false);
                      } /*if*/
                    LastChecked =
                        TheView instanceof android.widget.RadioButton ?
                            (android.widget.RadioButton)TheView
                        :
                            (android.widget.RadioButton)
                            ((android.view.ViewGroup)TheView).findViewById(R.id.file_item_checked);
                    CurSelected = MyItem;
                    MyItem.Selected = true;
                    LastChecked.setChecked(true);
                  } /*if*/
              } /*onClick*/
          } /*OnSetCheck*/

        SelectedItemAdapter
          (
            android.content.Context TheContext,
            int ResID,
            android.view.LayoutInflater TemplateInflater
          )
          {
            super(TheContext, ResID);
            this.ResID = ResID;
            this.TemplateInflater = TemplateInflater;
            CurSelected = null;
            LastChecked = null;
          } /*SelectedItemAdapter*/

        @Override
        public android.view.View getView
          (
            int Position,
            android.view.View ReuseView,
            android.view.ViewGroup Parent
          )
          {
            android.view.View TheView = ReuseView;
            if (TheView == null)
              {
                TheView = TemplateInflater.inflate(ResID, null);
              } /*if*/
            final PickerItem ThisItem = (PickerItem)this.getItem(Position);
            ((android.widget.TextView)TheView.findViewById(R.id.select_file_name))
                .setText(ThisItem.toString());
            final android.widget.RadioButton ThisChecked =
                (android.widget.RadioButton)TheView.findViewById(R.id.file_item_checked);
            ThisChecked.setChecked(ThisItem.Selected);
            final OnSetCheck ThisSetCheck = new OnSetCheck(ThisItem);
            ThisChecked.setOnClickListener(ThisSetCheck);
              /* otherwise radio button can get checked but I don't notice */
            TheView.setOnClickListener(ThisSetCheck);
            return
                TheView;
          } /*getView*/

      } /*SelectedItemAdapter*/

    @Override
    public void onCreate
      (
        android.os.Bundle savedInstanceState
      )
      {
        super.onCreate(savedInstanceState);
        Picker.Current = this;
        setContentView(R.layout.picker);
        PickerList = new SelectedItemAdapter(this, R.layout.picker_item, getLayoutInflater());
        PickerListView = (android.widget.ListView)findViewById(R.id.item_list);
        PickerListView.setAdapter(PickerList);
        PickerList.clear();
          {
            final String ExternalStorage =
                android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            for (String Here : LookIn)
              {
                final java.io.File ThisDir = new java.io.File(ExternalStorage + "/" + Here);
                if (ThisDir.isDirectory())
                  {
                    for (java.io.File Item : ThisDir.listFiles())
                      {
                        if (Item.getName().endsWith(".obj"))
                          {
                            PickerList.add(new PickerItem(Item.getAbsolutePath()));
                          } /*if*/
                      } /*for*/
                  } /* if*/
              } /*for*/
          }
        PickerList.notifyDataSetChanged();
        ((android.widget.Button)findViewById(R.id.item_select)).setOnClickListener
          (
            new android.view.View.OnClickListener()
              {
                public void onClick
                  (
                    android.view.View TheView
                  )
                  {
                    PickerItem Selected = null;
                    for (int i = 0;;)
                      {
                        if (i == PickerList.getCount())
                            break;
                        final PickerItem ThisItem =
                            (PickerItem)PickerListView.getItemAtPosition(i);
                        if (ThisItem.Selected)
                          {
                            Selected = ThisItem;
                            break;
                          } /*if*/
                        ++i;
                      } /*for*/
                    if (Selected != null)
                      {
                        setResult
                          (
                            android.app.Activity.RESULT_OK,
                            new android.content.Intent()
                                .setData
                                  (
                                    android.net.Uri.fromFile
                                      (
                                        new java.io.File(Selected.FullPath)
                                      )
                                  )
                          );
                        finish();
                      } /*if*/
                  } /*onClick*/
              } /*OnClickListener*/
          );
      } /*onCreate*/

    @Override
    public void onDestroy()
      {
        super.onDestroy();
        Picker.Current = null;
      } /*onDestroy*/

    public static void Launch
      (
        android.app.Activity Acting,
        int RequestCode,
        String[] LookIn /* array of names of subdirectories within external storage */
      )
      {
        if (!Reentered)
          {
            Reentered = true; /* until Picker activity terminates */
            Picker.LookIn = LookIn;
            Acting.startActivityForResult
              (
                new android.content.Intent(android.content.Intent.ACTION_PICK)
                    .setClass(Acting, Picker.class),
                RequestCode
              );
          }
        else
          {
          /* can happen if user gets impatient and selects from menu twice, just ignore */
          } /*if*/
      } /*Launch*/

    public static void Cleanup()
      /* Client must call this to do explicit cleanup; I tried doing it in
        onDestroy, but of course that gets called when user rotates screen,
        which means picker context is lost. */
      {
        LookIn = null;
        Reentered = false;
      } /*Cleanup*/

  } /*Picker*/
