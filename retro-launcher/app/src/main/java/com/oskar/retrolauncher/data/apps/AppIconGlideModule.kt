package com.oskar.retrolauncher.data.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.signature.ObjectKey

/**
 * T1.28 AC3 — Custom Glide model loader that resolves an `ApplicationInfo` to
 * its launcher icon `Drawable` via `PackageManager`. Cache key is the package
 * name so Glide's memory + disk caches dedupe icons across grid items.
 */
@GlideModule
class AppIconGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.append(
            ApplicationInfo::class.java,
            Drawable::class.java,
            AppIconLoaderFactory(context.packageManager),
        )
    }

    // The launcher has no other GlideModules in its manifest; disabling parsing
    // saves a small chunk of startup work.
    override fun isManifestParsingEnabled(): Boolean = false
}

private class AppIconLoaderFactory(
    private val pm: PackageManager,
) : ModelLoaderFactory<ApplicationInfo, Drawable> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ApplicationInfo, Drawable> =
        AppIconLoader(pm)

    override fun teardown() = Unit
}

private class AppIconLoader(
    private val pm: PackageManager,
) : ModelLoader<ApplicationInfo, Drawable> {
    override fun handles(model: ApplicationInfo): Boolean = true

    override fun buildLoadData(
        model: ApplicationInfo,
        width: Int,
        height: Int,
        options: Options,
    ): ModelLoader.LoadData<Drawable> {
        // Versioning the key on uid means a package upgrade busts the cache;
        // packageName alone would serve a stale icon after an OTA-style install.
        val key = ObjectKey("app-icon:${model.packageName}:${model.uid}")
        return ModelLoader.LoadData(key, AppIconFetcher(pm, model))
    }
}

private class AppIconFetcher(
    private val pm: PackageManager,
    private val info: ApplicationInfo,
) : DataFetcher<Drawable> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Drawable>) {
        try {
            callback.onDataReady(info.loadIcon(pm))
        } catch (t: Throwable) {
            callback.onLoadFailed(Exception("loadIcon failed for ${info.packageName}", t))
        }
    }

    override fun cleanup() = Unit
    override fun cancel() = Unit
    override fun getDataClass(): Class<Drawable> = Drawable::class.java
    override fun getDataSource(): DataSource = DataSource.LOCAL
}
