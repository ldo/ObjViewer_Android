package nz.gen.geek_central.ObjViewer; /* must be in same package as app in order to find resources */
/*
    let the user choose a file to load

    Copyright 2011-2014 by Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

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

import android.view.View;

public class Picker extends android.app.Activity
  {
  /* required extra information passed in launch intent: */
    public static final String LookInID = "nz.gen.geek_central.android.useful.Picker.LookIn";
      /* array of strings representing names of directories in which to look for files */
      /* ones not beginning with “/” are interpreted as subdirectories in external storage */
    public static final String ExtensionID = "nz.gen.geek_central.android.useful.Picker.Extension";
      /* extension of filenames to show */

    android.widget.ListView PickerListView;
    SelectedItemAdapter PickerList;

    static class PickerItem
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

        class OnSetCheck implements View.OnClickListener
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
                View TheView
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
        public View getView
          (
            int Position,
            View ReuseView,
            android.view.ViewGroup Parent
          )
          {
            View TheView = ReuseView;
            if (TheView == null)
              {
                TheView = TemplateInflater.inflate(ResID, null);
              } /*if*/
            final PickerItem ThisItem = this.getItem(Position);
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
        android.os.Bundle ToRestore
      )
      {
        super.onCreate(ToRestore);
        setContentView(R.layout.picker);
        PickerList = new SelectedItemAdapter(this, R.layout.picker_item, getLayoutInflater());
        PickerListView = (android.widget.ListView)findViewById(R.id.item_list);
        PickerListView.setAdapter(PickerList);
        PickerList.setNotifyOnChange(false);
        PickerList.clear();
          {
            final String ExternalStorage =
                android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            final String Extension = getIntent().getStringExtra(ExtensionID);
            for (String Here : getIntent().getStringArrayExtra(LookInID))
              {
                final java.io.File ThisDir = new java.io.File
                  (
                        (Here.startsWith("/") ?
                            ""
                        :
                            ExternalStorage + "/"
                        )
                    +
                        Here
                  );
                if (ThisDir.isDirectory())
                  {
                    for (java.io.File Item : ThisDir.listFiles())
                      {
                        if (Item.getName().endsWith(Extension))
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
            new View.OnClickListener()
              {
                public void onClick
                  (
                    View TheView
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

  } /*Picker*/
