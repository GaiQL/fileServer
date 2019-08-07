const exec = require('child_process').exec;
// const SMB2 = require("smb2");
// const SambaClient = require('samba-client');
const fs = require('fs');
const net = require('net');
const path = require('path');
const shell = require('shelljs');
const ProgressBar = require('progress');
const archiver = require('archiver');
const request = require('request');
const mineType = require('mime-types');
const compressing = require('compressing');

//  全局捕捉错误，发生错误后关闭 socket 连接；
// const bank = process.argv[2];
const bankId = 248;
const envType = process.argv[3];
const bankVersion = process.argv[4];

let cookieData = '';
let warFilePath = './248';

const requestAction = ( params,type ) => {
  return new Promise((resolve, reject) => {
      request({
            method: 'POST',
            headers:{
              'Content-Type' : 'application/x-www-form-urlencoded; charset=UTF-8',
              'Cookie' : cookieData
            },
            ...params
        }, function(error, response, body) {
          // console.log( error )
          // errCode:'1'   && !JSON.parse(body).errCode
          if( error || !response ){ reject( error ) };
          if ( !error && response.statusCode == 200 && ( typeof body == 'string'?!JSON.parse(body).errCode:true ) ) {
            // body.cookieData = response.headers['set-cookie'];
            if( type == 'login' ){
              response.headers['set-cookie'].forEach((cookie)=>{ cookieData += `${cookie.split(';')[0]};` })
            }
            resolve( body );
          }else if( response.statusCode == 302 ){
            // 重新登录；
            console.log( '要重新登录' );
          }else{
            reject( error );
          }
        })
      })
      .then((res)=>{ return typeof res  == 'string'?JSON.parse(res):res })
      .catch((err)=>{ throw err })
}

let pageLimit = 12;
let pageNum = 1;
const getAppVersion = async () => {
  
  const loginData = await requestAction({ url:'http://39.105.208.219:8002/admin/login/userLogin',formData:{loginName:'admin',passWord:'123456'} },'login');
  
  let listData = await requestAction({
    method: 'GET',
    url:'http://39.105.208.219:8002/admin/edition/query',
    qs:{ page:pageNum,limit:pageLimit }
  });

  if( !listData.data.length ){ return };

  listData.data.forEach(( e,i )=>{
    if( e.orgId == bankId ){
      appVersion = e.version;
      appListId = e.id;
    }
  })
  
  if( !appVersion ){ ++pageNum;getAppVersion() };

}

let appListId = 0;
let appVersion = '';
let H5Version = '';
let defaultVersion = '1.0.0'.split('.');
const getVersion = async () => {

  let versionData = await requestAction({
    url:'http://localhost:8080/getH5Version', 
    body:{ fileName:248 },
    headers:{ 'Content-Type' : "application/json" },
    json:true
  })
  H5Version = versionData.app?versionData.app.version:'';
  console.log( 'H5版本：' + ( H5Version?H5Version:'无' ) );
  await getAppVersion();
  console.log( 'APP版本：' + ( appVersion?appVersion:'无' ) );

  let appVersionArr = appVersion?appVersion.split('.'):defaultVersion;
  let H5VersionArr = H5Version?H5Version.split('.'):defaultVersion;
  let relativelyHeightArr = null;

  appVersionArr.some(( e,i )=>{
    
    if( Number(e) > Number(H5VersionArr[i]) ){
      relativelyHeightArr = appVersionArr
      return true;
    }
    
  })

  if( !relativelyHeightArr ){ relativelyHeightArr = H5VersionArr };

  relativelyHeightArr = relativelyHeightArr.map( e => Number( e ) );

  let currentNum = relativelyHeightArr.length - 1;
  let numLimit = 20;
  let upgradeOnoff = false;
  let calculateVersion = () => {
    if( !currentNum ){
      upgradeOnoff?++relativelyHeightArr[currentNum]:null;
      return;
    }
    if( upgradeOnoff ){ ++relativelyHeightArr[currentNum] };
    if( ( upgradeOnoff?relativelyHeightArr[currentNum]:relativelyHeightArr[currentNum] + 1 ) > numLimit ){
      relativelyHeightArr[currentNum] = 0;
      upgradeOnoff = true;
    }else{
      if( currentNum == relativelyHeightArr.length - 1 ){ ++relativelyHeightArr[currentNum] };
      upgradeOnoff = false;
    }
    --currentNum;
    calculateVersion()
  }
  calculateVersion();
  return relativelyHeightArr.join('.');
  
}

const deleteDir = (url) =>{

  if( fs.existsSync(url) ) {

      if( /.zip$/.test(url) ){ fs.unlinkSync(url);return; }

      let files = fs.readdirSync(url);   
      files.forEach(function(file,index){

          let curPath = path.join( url,file );
          if( fs.statSync(curPath).isDirectory() ) {
              deleteDir(curPath);
          } else {
              fs.unlinkSync(curPath); 
          }
              
      });
      fs.rmdirSync(url); 
  }

}

( async () => {

  let version = await getVersion();
  console.log( '此次打包版本：',version );

  //  本地清理及打包。。。
  deleteDir( './build' );
  deleteDir( warFilePath );
  deleteDir( `${ warFilePath }.zip` );

  // let cli = `npm run buildCommand ${bankId} ${envType} ${bankVersion}`
  let buildCli = `npm run build 248 test4 ${version}`;console.log( buildCli );

  if (shell.exec( buildCli ).code !== 0){
      shell.echo('build failed');
      shell.exit(1);
  }
  fs.renameSync( './build', warFilePath );

  let zipCli = `gulp beginZip --bankId ${bankId}`;console.log( zipCli );

  if (shell.exec( zipCli ).code !== 0){
      shell.echo('zip failed');
      shell.exit(1);
  }
  deleteDir('./temporaryDir');


  // exec(cli,function (err,stdout,stderr){
  //     if (err){
  //         console.log( err );
  //         return;
  //     }
  //     // console.log('stdout'+stdout);
  //     // console.log('stderr'+stderr);

  //     console.log( '打包完成2' );

  // })

  let fileNum = 0;
  const calculateFile = ( filePath ) => {
    let allFile = fs.readdirSync(filePath);
    allFile.length && allFile.forEach(( e )=>{
      let fileDir = path.join( filePath,e );
      let fileStat = fs.statSync( fileDir );
  
      if( fileStat.isFile() ){ ++fileNum }
      if( fileStat.isDirectory() ){ calculateFile(fileDir) }
    })
  }
  calculateFile( warFilePath );

  let bar = new ProgressBar('uploading [:bar] :current/:total :percent', {
    total: fileNum,
    width: 50,
  });

    let port = 9999;
    let host = '127.0.0.1';
    let client= new net.Socket();

    client.setEncoding('utf-8');
    client.connect(port,host,function(){


      let totalLength = 0;
      let bufferArr = [];
      
      let readStream = fs.createReadStream('./248.zip');
      readStream.on('data', function( buffer ) {     //65536      //131701

        // console.log( buffer.length )
        bufferArr.push( buffer );
        totalLength += buffer.length;
        // client.write( buffer ) 
        
      });
      readStream.on('end', function() {
        
        let buffer = Buffer.concat( bufferArr , totalLength );
        client.write( buffer )

        let outputBuf = Buffer.alloc( 100 );
        outputBuf.write( JSON.stringify({ type:'/end',name:'248',id:appListId,version }) );
        client.write( outputBuf );
        
      });
      readStream.on('error', function( error ){ console.error('readStream error:', error.message) })

    });
    client.on('data',function(data){

      try {

        data = JSON.parse( data );
        switch (data.type) {
          case 'text':console.log( data.message );break;
          case 'upload':bar.tick();break;
          default: break;
        }

      } catch(e) {
        throw 'error';
      }
      
    });
    client.on('error',function(error){ throw error });
    client.on('close',function(){ console.log('Connection closed');});

})();

return;











// request方法；
// const requestAction = ( params,type ) => {
//   return new Promise((resolve, reject) => {
//       request({
//             method: 'POST',
//             headers:{
//               'Content-Type' : 'application/x-www-form-urlencoded; charset=UTF-8',
//               'Cookie' : cookieData
//             },
//             ...params
//         }, function(error, response, body) {
//           // console.log( error )
//           // errCode:'1'   && !JSON.parse(body).errCode
//           if( error || !response ){ reject( error ) };
//           if ( !error && response.statusCode == 200 && ( typeof body == 'string'?!JSON.parse(body).errCode:true ) ) {
//             // body.cookieData = response.headers['set-cookie'];
//             if( type == 'login' ){
//               response.headers['set-cookie'].forEach((cookie)=>{ cookieData += `${cookie.split(';')[0]};` })
//             }
//             resolve( body );
//           }else if( response.statusCode == 302 ){
//             // 重新登录；
//             console.log( '要重新登录' );
//           }else{
//             reject( error );
//           }
//         })
//       })
//       .then((res)=>{ return typeof res  == 'string'?JSON.parse(res):res })
//       .catch((err)=>{ throw err })
// }



//获取app班恩

// const getAppVersion = async () => {
  
//   const loginData = await requestAction({ url:'http://39.105.208.219:8002/admin/login/userLogin',formData:{loginName:'admin',passWord:'123456'} },'login');
  
//   let listData = await requestAction({
//     method: 'GET',
//     url:'http://39.105.208.219:8002/admin/edition/query',
//     qs:{ page:pageNum,limit:pageLimit }
//   });

//   if( !listData.data.length ){ return };

//   listData.data.forEach(( e,i )=>{
//     if( e.orgId == bankId ){
//       appVersion = e.version;
//       appListId = e.id;
//     }
//   })
  
//   if( !appVersion ){ ++pageNum;getAppVersion() };

// }

//  上传app
  
  // const loginData = await requestAction({ url:'http://39.105.208.219:8002/admin/login/userLogin',formData:{loginName:'admin',passWord:'123456'} },'login');

  // console.log( '登录管理后台成功，开始上传...' );

  // let readStream = fs.createReadStream('./248.zip', { encoding : 'base64' })
  // let fileBase64DataAll = 'data:application/x-zip-compressed;base64,';
  // readStream.on('data', function( base64Data ) {

  //   fileBase64DataAll += base64Data;
    
  // });
  // readStream.on('end', async function() {
  //   const uploadData = await requestAction({
  //     url:'http://39.105.208.219:8002/admin/edition/update',
  //     headers:{
  //       'Accept-Encoding': 'gzip, deflate',
  //       'Accept-Language': 'zh-CN,zh;q=0.9',
  //       'Cache-Control': 'no-cache',
  //       'Content-Type' : 'application/x-www-form-urlencoded; charset=UTF-8',
  //       'Connection': 'keep-alive',
  //       'Pragma':'no-cache',
  //       'Cookie' : cookieData
  //     },
  //     formData:{
  //       id: appListId,
  //       orgId: '248',
  //       version,
  //       status: 'test4',
  //       fileName: '248.zip',
  //       downloaUrl:fileBase64DataAll
  //     }
  //   });
  //   console.log(`APP发版成功，列表id为--${appListId}`);
  // });
  // readStream.on('error', function( error ){ console.error('readStream error:', error.message) })