package com.bsk.update1;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
//import android.os.storage.StorageVolume;

import com.bsk.update.util.SwFile;
import com.bsk.update1.R;

//这个类就是用来打开SD卡目录
public class FileSelector extends Activity implements OnItemClickListener {

	public static final String FILE = "file";
	public static final String ROOT = "root";

	private File mCurrentDirectory;

	private LayoutInflater mInflater;

	private FileAdapter mAdapter = new FileAdapter();

	private ListView mListView;

	private String mRootPath;

	private StorageManager mStorageManager;
	// private StorageVolume[] mVolumes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 加载布局管理器,不加载就会出错，不知道为什么
		mInflater = LayoutInflater.from(this);
		setContentView(R.layout.file_list);
		// mListView=ListView
		mListView = (ListView) findViewById(R.id.file_list);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
		// mVolumes = mStorageManager.getVolumeList();
		
		Intent intent = getIntent();
		//rootPath="/mnt"
		String rootPath = intent.getStringExtra(ROOT);
		// 确实不等于null，rootPath="/mnt"
		if (rootPath != null) {
			// mAdapter.setCurrentList(new File(rootPath).listFiles(new
			// VolumeFilter()));
			
			//最后返回：/mnt/sdcard
			mAdapter.setCurrentList(getRootFiles());
			// mRootPath：一个String类型
			mRootPath = rootPath;
		} else {
			// RESULT_CANCELED=0，从上一个Activity传过来的数据为空的话，才传回去，不然不传回去
			//RESULT_CANCELED：代表取消,就是当前焦点不是当前对话框时返回数据给OtaFragment
			setResult(RESULT_CANCELED);
		}
	}

	public File[] getRootFiles() {
		//getRootPaths()返回/mnt/sdcard，pathsList=[/mnt/sdcard]
		List<String> pathsList = getRootPaths();
		//files=[null],size=1
		File[] files = new File[pathsList.size()];
		for (int i = 0; i < pathsList.size(); i++) {
			files[i] = new File(pathsList.get(i));
		}
		//files=[/mnt/sdcard]
		return files;
	}

	//[/mnt/sdcard, /mnt/extsd, /mnt/usbhost1]
	public List<String> getRootPaths() {
		List<String> pathsList = new ArrayList<String>();
		try {
			//获取路径
			Method method = StorageManager.class.getDeclaredMethod("getVolumePaths");
			method.setAccessible(true);
			//result=[/mnt/sdcard, /mnt/extsd, /mnt/usbhost1]
			Object result = method.invoke(mStorageManager);
			if (result != null && result instanceof String[]) {
				String[] pathes = (String[]) result;
				StatFs statFs;
				for (String path : pathes) {
					if (!TextUtils.isEmpty(path) && new File(path).exists()) {
						statFs = new StatFs(path);
						if (statFs.getBlockCount() * statFs.getBlockSize() != 0) {
							pathsList.add(path);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			File externalFolder = Environment.getExternalStorageDirectory();
			if (externalFolder != null) {
				pathsList.add(externalFolder.getAbsolutePath());
			}
		}
		System.out.println(pathsList);
		//最后返回pathsList=[/mnt/sdcard]，因为其他两个路径为空
		return pathsList;
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		File selectFile = (File) adapterView.getItemAtPosition(position);
		if (selectFile.isDirectory()) {
			//selectFile=/mnt/sdcard
			mCurrentDirectory = selectFile;
			//显示目录的数据
			FileAdapter adapter = (FileAdapter) adapterView.getAdapter();
			adapter.setCurrentList(selectFile.listFiles(new ZipFilter()));
		}
		//如果是一个文件的话，就返回数据给OtaFragment
		else if (selectFile.isFile()) {
			Intent intent = new Intent();
			//返回文件路径给OtaFragment
			intent.putExtra(FILE, selectFile.getPath());
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		if (mCurrentDirectory == null || mCurrentDirectory.getPath().equals(mRootPath)) {
			super.onBackPressed();
		} else {
			mCurrentDirectory = mCurrentDirectory.getParentFile();
			if (mCurrentDirectory.getPath().equals(mRootPath)) {
				// mAdapter.setCurrentList(mCurrentDirectory.listFiles(new
				// VolumeFilter()));
				mAdapter.setCurrentList(getRootFiles());
			} else {
				mAdapter.setCurrentList(mCurrentDirectory.listFiles(new ZipFilter()));
			}
		}
	}

	//file=/mnt/sdcard
	public boolean isVolume(File file) {
		//pathsList机器上读取到的路径
		//file为适配器上的路径
		List<String> pathsList = getRootPaths();
		for (int i = 0; i < pathsList.size(); i++) {
			//file.getAbsolutePath()再一次从机器上获取路径
			if (file.getAbsolutePath().equals(pathsList.get(i))) {
				return true;
			}
		}
		return false;
	}

	private class FileAdapter extends BaseAdapter {

		private File mFiles[];

		private class ViewHolder {
			ImageView image;
			TextView fileName;
			TextView fileSize;
		}

		public void setCurrentList(File directory) {
			mFiles = directory.listFiles();
			notifyDataSetChanged();
		}

		//files：/mnt/sdcard
		public void setCurrentList(File[] files) {
			mFiles = files;
			
			//动态更新ListView 
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mFiles == null ? 0 : mFiles.length;
		}

		@Override
		public File getItem(int position) {
			File file = mFiles == null ? null : mFiles[position];
			return file;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				//convertView=RelativeLayout，因为file_list_item为RelativeLayout布局
				convertView = mInflater.inflate(R.layout.file_list_item, null);
				//最快的方式是定义一个ViewHolder，将convetView的tag设置为ViewHolder,不为空时重新使用即可
				ViewHolder holder = new ViewHolder();
				holder.image = (ImageView) convertView.findViewById(R.id.image);
				holder.fileName = (TextView) convertView.findViewById(R.id.file_name);
				holder.fileSize = (TextView) convertView.findViewById(R.id.file_size);
				convertView.setTag(holder);
			}
			ViewHolder holder = (ViewHolder) convertView.getTag();
			//mFiles就是一个列表，里面存了：/mnt/sdcard
			//position=0，file=/mnt/sdcard
			File file = mFiles[position];
			//isVolume(file)应该是存在/mnt/sdcard时，就进入
			if (isVolume(file)) {
				holder.fileSize.setText("");
				holder.image.setImageResource(R.drawable.litter_disk);
			} else if (file.isDirectory()) {
				holder.fileSize.setText("");
				holder.image.setImageResource(R.drawable.litter_file);
			} else if (file.isFile()) {
				holder.fileSize.setText(SwFile.byteToSize(file.length()));
				holder.image.setImageResource(R.drawable.litter_zip);
			}
			holder.fileName.setText(file.getName());
			return convertView;
		}

	}

	//过滤器，只显示.ZIP的文件
	private class ZipFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			if (pathname != null) {
				if (pathname.isDirectory()) {
					return true;
				}
				String path = pathname.getPath().toLowerCase();
				return path.endsWith(".zip");
				
			}
			return false;
		}

	}

}
