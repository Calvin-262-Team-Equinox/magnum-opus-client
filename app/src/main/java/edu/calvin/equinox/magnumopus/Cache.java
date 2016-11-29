package edu.calvin.equinox.magnumopus;


import android.support.annotation.Nullable;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public enum Cache
{
    INSTANCE;

    private static final int CACHE_VERSION = 1;
    private DiskLruCache m_lruCache = null;

    public void init(File dir)
    {
        if (m_lruCache != null)
        {
            return;
        }

        try
        {
            m_lruCache = DiskLruCache.open(
                    dir,
                    CACHE_VERSION,
                    1,
                    1024 * 1024 // 1 MB
            );
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Nullable
    public byte[] get(String key)
    {
        if (m_lruCache == null)
        {
            return null;
        }

        byte[] value = null;
        try
        {
            DiskLruCache.Snapshot snapshot = m_lruCache.get(key);
            if (snapshot != null)
            {
                InputStream is = snapshot.getInputStream(0);
                value = new byte[(int)snapshot.getLength(0)];
                if (is.read(value) != value.length)
                {
                    value = null;
                }
                is.close();

                snapshot.close();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return value;
    }

    public void put(String key, byte[] value)
    {
        if (m_lruCache == null)
        {
            return;
        }

        try
        {
            DiskLruCache.Editor editor = m_lruCache.edit(key);
            if (editor != null)
            {
                OutputStream os = editor.newOutputStream(0);
                os.write(value);
                os.close();
                editor.commit();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
