'use strict'
const path = require('path')
const defaultSettings = require('./src/settings.js')

const CopyWebpackPlugin = require('copy-webpack-plugin')
// const CompressionPlugin = require('compression-webpack-plugin')
// const webpack = require('webpack')
// const AddAssetHtmlPlugin = require('add-asset-html-webpack-plugin')
function resolve(dir) {
  return path.join(__dirname, dir)
}

const name = defaultSettings.title || 'vue Admin Template' // page title

const port = process.env.port || process.env.npm_config_port || 9528 // dev port
const parallel = process.env.NODE_ENV === 'development'
module.exports = {
  productionSourceMap: true,
  parallel,
  // 使用mock-server
  devServer: {
    host: '0.0.0.0', // 配置本项目运行主机
    port: 8080, // 配置本项目运行端口
    // 配置代理服务器来解决跨域问题
    proxy: {
      '/dev-api/static': {
        target: 'http://localhost:8080', // 配置要替换的后台接口地址
        changOrigin: true, // 配置允许改变Origin
        ws: true, // proxy websockets
        pathRewrite: {
          '^/dev-api/static': '/static/'
        }
      },
      '/dev-api': {
        target: 'https://de.fit2cloud.com', // 配置要替换的后台接口地址
        changOrigin: true, // 配置允许改变Origin
        ws: true, // proxy websockets
        pathRewrite: {
          '^/dev-api': '/'
        }
      },
      '/websocket': {
        target: 'https://de.fit2cloud.com', // 配置要替换的后台接口地址
        changOrigin: true, // 配置允许改变Origin
        ws: true // proxy websockets
      }
    }
  },

  pages: {
    index: {
      entry: 'src/main.js',
      template: 'public/index.html',
      filename: 'index.html'
    }
  },
  configureWebpack: {
    name: name,
    devtool: 'source-map',
    resolve: {
      alias: {
        '@': resolve('src')
      }
    },
    plugins: [
      new CopyWebpackPlugin([
        {
          from: path.join(__dirname, 'static'),
          to: path.join(__dirname, 'dist/static')
        }
      ]),
      // new webpack.DllReferencePlugin({
      //   context: process.cwd(),
      //   manifest: require('./public/vendor/vendor-manifest.json')
      // }),
      // // 将 dll 注入到 生成的 html 模板中
      // new AddAssetHtmlPlugin({
      //   // dll文件位置
      //   filepath: path.resolve(__dirname, './public/vendor/*.js'),
      //   // dll 引用路径
      //   publicPath: './vendor',
      //   // dll最终输出的目录
      //   outputPath: './vendor'
      // })
    ]
  },
  chainWebpack: config => {
    config.module.rules.delete('svg') // 删除默认配置中处理svg,
    // const svgRule = config.module.rule('svg')
    // svgRule.uses.clear()
    config.module
      .rule('svg-sprite-loader')
      .test(/\.svg$/)
      .include
      .add(resolve('src/icons')) // 处理svg目录
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]'
      })
    if (process.env.NODE_ENV === 'production') {
      /* config.plugin('compressionPlugin').use(new CompressionPlugin({
        test: /\.(js|css|less)$/, // 匹配文件名
        threshold: 10240, // 对超过10k的数据压缩
        minRatio: 0.8,
        deleteOriginalAssets: true // 删除源文件
      })) */
    }

config.module
      .rule('icons')
      .test(/\.svg$/)
      .include.add(resolve('src/deicons'))
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: '[name]'
      })


  },
  css: {
    loaderOptions: {
      sass: {
        prependData: `@import "@/style/index.scss"`
      }
    }
  }

}



