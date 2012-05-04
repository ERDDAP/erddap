////NO need to edit /////////////

/***********************************************
* Swiss Army Image slide show script  - © John Davenport Scheuer: http://home.comcast.net/~jscheuer1/
* This notice MUST stay intact for legal use
* Visit Dynamic Drive at http://www.dynamicdrive.com/ for full original source code
***********************************************/

function preloadctrl(im){
if (typeof im=='string'){
var imo=new Image();
imo.src=im;
}
}

if(document.images&&typeof preload_ctrl_images!='undefined'&&preload_ctrl_images){
var ctrlimgs=[previmg, stopimg, playimg, nextimg]
for (var i_tem = 0; i_tem < ctrlimgs.length; i_tem++)
if (ctrlimgs[i_tem])
preloadctrl(ctrlimgs[i_tem])
}

var iss=[]; //array to cache inter_slide instances

function inter_slide(){
if(!document.images||arguments.length==0)
return;
var imgs=arguments[0];
var width=null, height=null, delay=null;
if(arguments.length==2)
delay=arguments[1];
else if(arguments.length==3||arguments.length==4)
width=arguments[1], height=arguments[2], delay=arguments[3]? arguments[3] : null;
this.dom=(document.getElementById) //modern dom browsers
this.da=document.all
this.delay=imgs.delay? imgs.delay : delay? delay : 3000+iss.length*750;
this.nextimgidx=0;
iss[this.issid=iss.length]=this;
this.imgs=imgs;
if (imgs.random)
this.imgs.sort(function() {return 0.5 - Math.random();}) //thanks to Mike (aka Mwinter) :)
this.imgborder=imgs.border? parseInt(imgs.border) : 0;
if (!this.dom){
this.postimgs=new Array() //preload imgs
for (p=0;p<imgs.length;p++){
this.postimgs[p]=new Image()
this.postimgs[p].src=this.imgs[p][0]
}
}

if (this.dom){ //if modern browsers (ie: Firefox, IE5+)
this.faded=0;
this.loadimgidx=[];
this.fade=!imgs.nofade;
if(this.fade)
this.degree=10 //initial opacity degree (10%)
this.pausecheck=imgs.pause;
this.mouseovercheck=0
this.canvasbase="canvas"+this.issid
this.curcanvas=this.canvasbase+"_0"
this.descriptions=!imgs.no_descriptions;
this.man_start=imgs.manual_start;
this.addbr=!imgs.no_added_linebreaks;
this.no_auto=imgs.no_auto;
this.onclick=imgs.onclick;
this.specs=imgs.specs;
this.counter=imgs.counter;
this.ics=imgs.image_controls;
this.jumpto=imgs.jumpto;
this.no_c=imgs.no_controls;
this.target=imgs.target;
this.ualt=imgs.use_alt;
this.utit=imgs.use_title;
this.fadecolor=imgs.fadecolor;
this.ibut_hc=imgs.button_highlight;
this.dp=imgs.desc_prefix? imgs.desc_prefix : ' ';
this.imbcolor=imgs.border_color;
this.imbstyle=imgs.border_style;
this.width=imgs.width? imgs.width : width? width : null
this.width=this.width? this.width+this.imgborder*2 : null;
this.height=imgs.height? imgs.height : height? height : null
this.height=this.height? this.height+this.imgborder*2 : null;
var ief='', dims='';
if(this.width||this.height)
dims=this.width&&this.height? 'width:'+this.width+'px;height:'+this.height+'px;' : this.width? 'width:'+this.width+'px;' : 'height:'+this.height+'px;';
if(this.fade&&document.body&&document.body.style)
ief=document.body.filters? 'filter:progid:DXImageTransform.Microsoft.alpha(opacity=0);' : typeof document.body.style.opacity=='string'? 'opacity:0;' : 'opacity:0.10;-moz-opacity:0.10;-khtml-opacity:0.10;';
var brs=this.addbr? '<br>' : '';
if (imgs.controls_top){
this.controls();
document.write(brs)
}
document.write('<div align="center"><div id="master'+this.issid+'" style="position:relative;'+dims+'overflow:hidden;margin:0 auto;"><div id="'+this.canvasbase+'_0" style="position:absolute;'+dims+'top:0;left:0;'+ief+'background-color:'+(imgs.fadecolor? imgs.fadecolor : 'white')+'"><\/div><div id="'+this.canvasbase+'_1" style="position:absolute;'+dims+'top:0;left:0;'+ief+'background-color:'+(imgs.fadecolor? imgs.fadecolor : 'white')+'"><\/div><\/div><\/div>')
if(this.descriptions)
document.write(brs+'<div align="center">'+this.dp+'<span id="imgdsc'+this.issid+'">&nbsp;<\/span></div>\n');
if(this.counter&&!this.descriptions)
document.write(brs);
if(this.counter)
this.cntrl()
if (!imgs.controls_top){
document.write(this.ics? '' : brs);
this.controls();
}
}
else
document.write('<div align="center"><img name="defaultslide'+this.issid+'" src="'+this.postimgs[0].src+'"><\/div>')


if (this.dom) //if modern browsers such as Firefox, IE5+
this.startit()
else{
this.nextimgidx++
setInterval("iss["+this.issid+"].rotateimg()", this.delay)
}
}

inter_slide.prototype.cntrl=function(){
this.cpad=this.imgs.length.toString().length;
document.write('<div align="center">Viewing Image: <b><span id="thecnt'+this.issid+'"><\/span><\/b> of<span style="font-size:1ex;">&nbsp;</span>&nbsp;<b>'+this.imgs.length+'<\/b><\/div>');
}

inter_slide.prototype.controls=function(){
var brs=this.addbr? '<br>' : ' &nbsp;';
if(!this.ics&&!this.no_c)
document.write('<div align="center"><input id="prev'+this.issid+'" disabled type="button" value=" << " onclick="iss['+this.issid+'].changeimg(false, \'nav\');">\n'+
(this.no_auto? '&nbsp;&nbsp;&nbsp;' : ' <input id="gostp'+this.issid+'" type="button" value=" Stop " onclick="iss['+this.issid+'].gostop(this);">\n')+
' <input id="next'+this.issid+'" disabled type="button" value=" >> " onclick="iss['+this.issid+'].changeimg(true, \'nav\');">'+(this.jumpto? brs+'Jump to: <input disabled type="text" size="'+this.imgs.length.toString().length+'" id="goto'+this.issid+'" onkeydown="iss['+this.issid+'].jumper(event);"><input id="go'+this.issid+'" disabled type="button" onclick="iss['+this.issid+'].jumper();" value="GO">' : '')+'<\/div>')
else if(this.ics&&!this.no_c){
var op=document.body.filters? ' style="filter:alpha(opacity=100);"' : ' style="opacity:'+(this.man_start? 0.99 : 0.45)+';"';
document.write('<table align="center" cellpadding="0" cellspacing="10"><tr><td valign="middle" align="center" style="padding:2px 2px 1px 2px;"><input'+op+' onmouseover="iss['+this.issid+'].ibute(this,1);" onmouseout="iss['+this.issid+'].ibute(this,2);" onmousedown="iss['+this.issid+'].ibute(this,3);" onmouseup="iss['+this.issid+'].ibute(this,4);" type="image" title="Previous" id="prev'+this.issid+'" src="'+previmg+'" onclick="iss['+this.issid+'].changeimg(false, \'nav\');"><\/td>'+(this.no_auto? '<td>&nbsp;<\/td>' : '<td valign="middle" align="center" style="padding:2px 2px 1px 2px;"><input onmouseover="iss['+this.issid+'].ibute(this,1);" onmouseout="iss['+this.issid+'].ibute(this,2);" onmousedown="iss['+this.issid+'].ibute(this,3);" onmouseup="iss['+this.issid+'].ibute(this,4);" title="Stop" type="image" id="gostp'+this.issid+'" src="'+(this.g_fPlayMode? playimg : stopimg)+'" onclick="iss['+this.issid+'].gostop(this);"><\/td>')+'<td valign="middle" align="center" style="padding:2px 2px 1px 2px;"><input'+op+' onmouseover="iss['+this.issid+'].ibute(this,1);" onmouseout="iss['+this.issid+'].ibute(this,2);" onmousedown="iss['+this.issid+'].ibute(this,3);" onmouseup="iss['+this.issid+'].ibute(this,4);" type="image" title="Next" id="next'+this.issid+'" src="'+nextimg+'" onclick="iss['+this.issid+'].changeimg(true, \'nav\');"><\/td><\/tr><\/table>\n');
}
else
this.no_c=1;
}

inter_slide.prototype.jumper=function(e){
var key;
if(typeof e=='object')
key=e.keyCode? e.keyCode : e.which? e.which : 1;
if(key&&key!==13)
return;
var num=typeof e=='number'? e : parseInt(this.go('goto'+this.issid).value);
if(num<=this.imgs.length&&num>0){
this.nextimgidx=num-2;
this.changeimg(true, 'jump');
}
}

inter_slide.prototype.ibute=function(obj, i){
if(!obj.parentNode)
return;
if(i==1)
obj.parentNode.style.backgroundColor=this.ibut_hc? this.ibut_hc : 'yellow';
else if(i==2)
obj.parentNode.style.backgroundColor='transparent';
else if(i==3){
obj.parentNode.style.borderTop=obj.parentNode.style.borderLeft='1px solid gray';
obj.parentNode.style.paddingRight='1px';obj.parentNode.style.paddingBottom=0;
}
if (i==2||i==4){
obj.parentNode.style.borderTop=obj.parentNode.style.borderLeft='none';
obj.parentNode.style.paddingRight='2px';obj.parentNode.style.paddingBottom='1px';
}
}

inter_slide.prototype.fadepic=function(){
if (this.fade&&this.degree<100){
this.faded=0
this.degree+=10
if (this.tempobj.filters&&this.tempobj.filters[0]){
if (typeof this.tempobj.filters[0].opacity=="number") //if IE6+
this.tempobj.filters[0].opacity=this.degree
else //else if IE5.5-
this.tempobj.style.filter="alpha(opacity="+this.degree+")"
}
else if (this.tempobj.style.MozOpacity)
this.tempobj.style.MozOpacity=this.degree/101
else if (this.tempobj.style.KhtmlOpacity)
this.tempobj.style.KhtmlOpacity=this.degree/100
else if (this.tempobj.style.opacity&&!this.tempobj.filters)
this.tempobj.style.opacity=this.degree/101
}
else{
this.faded=1
clearInterval(this.fadeclear)
this.nextcanvas=(this.curcanvas==this.canvasbase+"_0")? this.canvasbase+"_0" : this.canvasbase+"_1"
this.tempobj=this.go(this.nextcanvas)
if(this.playing)
this.changeimg()
}
}

inter_slide.prototype.populateslide=function(picobj, picidx){
if(document.getElementsByTagName){
if(picobj.getElementsByTagName('a')[0]&&picobj.getElementsByTagName('a')[0].onclick)
picobj.getElementsByTagName('a')[0].onclick=null;
if(picobj.getElementsByTagName('img')[0]&&picobj.getElementsByTagName('img')[0].onload)
picobj.getElementsByTagName('img')[0].onload=null;
}
picobj.style.backgroundColor=this.imgs[picidx].fadecolor? this.imgs[picidx].fadecolor : this.fadecolor? this.fadecolor : 'white';
var slideHTML='<table border="0" cellpadding="0" cellspacing="0" width="100%" height="100%"><tr><td width="100%" height="100%" align="center" valign="middle" style="background:transparent none;">'
if (this.imgs[picidx][2]){ //if associated link exists for img
var specs=this.imgs[picidx][4]? ", '"+this.imgs[picidx][4]+"'" : this.specs? ", '"+this.specs+"'" : '';
slideHTML+='<a href="'+this.imgs[picidx][2]+'"'+(this.imgs[picidx][3]? ' target="'+this.imgs[picidx][3]+'"' : this.target? ' target="'+this.target+'"' : '')+' onclick="'+(this.onclick? this.onclick : 'window.open(this.href, (this.target? this.target : \'_self\')'+specs+');return false;')+'">'
}
slideHTML+='<img id="theimg'+picidx+'_'+this.issid+'" src="'+(this.loadimgidx[picidx]&&typeof this.loadimgidx[picidx].complete=='boolean'&&this.loadimgidx[picidx].complete? this.loadimgidx[picidx].src : this.imgs[picidx][0])+'" alt="'+(this.ualt? this.imgs[picidx][1] : 'Slide Show Image')+'" title="'+(this.utit? this.imgs[picidx][1] : '')+'" '+(this.imbcolor&&!this.imgs[picidx].noborder? 'style="border:'+this.imgborder+'px '+(this.imbstyle? this.imbstyle : 'solid')+' '+this.imbcolor+';"' : 'border="'+(this.imgs[picidx].noborder? '0' : this.imgborder)+'"')+(!this.width||!this.height? ' onload="iss['+this.issid+'].imgload(this);"' : '')+'>'
if (this.imgs[picidx][2]) //if associated link exists for img
slideHTML+='<\/a>'
slideHTML+='<\/td><\/tr><\/table>'
picobj.innerHTML=slideHTML
}

inter_slide.prototype.buttons=function(bool){
if(this.no_c)
return;
var next=this.go('next'+this.issid), prev=this.go('prev'+this.issid);
next.disabled=prev.disabled=!bool;
next.title=bool? 'next' : '';
prev.title=bool? 'previous' : '';
if(this.jumpto&&!this.ics)
this.go('go'+this.issid).disabled=this.go('goto'+this.issid).disabled=!bool;
if(this.ics){
if(!this.no_auto){
var go=this.go('gostp'+this.issid)
go.title=bool? 'Play' : 'Stop';
go.src=bool? playimg : stopimg;
}
if(prev.filters&&prev.parentNode)
prev.parentNode.style.filter=next.parentNode.style.filter='alpha(opacity='+(bool? 100 : 45)+')';
else if(prev.style.opacity&&!prev.filters)
prev.style.opacity=next.style.opacity=bool? 0.99 : 0.45;
}
}

inter_slide.prototype.imgload=function(el){
if(!el&&!this.imgel)
return;
var el=el? el : this.imgel;
if(el.width==0||el.height==0){
this.imgel=el;
var cacheobj=this;
clearTimeout(this.getdim)
this.getdim=setTimeout(function(){cacheobj.imgload()}, 300)
return;
}
var m=this.go('master'+this.issid).style, c0=this.go(this.canvasbase+'_0').style, c1=this.go(this.canvasbase+'_1').style;
if(!this.width){
this.width=el.width+this.imgborder*2;
m.width = c0.width = c1.width = this.width+'px';
}
if(!this.height){
this.height=el.height+this.imgborder*2;
m.height = c0.height = c1.height = this.height+'px';
}
}

inter_slide.prototype.changeimg=function(bool,nav){
if(this.playing){
this.buttons(false)
this.nextimgidx=(this.keeptrack()<this.imgs.length-1)? this.keeptrack()+1 : 0
this.populateslide(this.tempobj, this.nextimgidx)
if(bool)
this.rotateimg()
else{
clearTimeout(this.inprocess)
this.inprocess=setTimeout("iss["+this.issid+"].rotateimg()", this.delay)
}
}
else {
if(!this.faded){
if(nav&&nav=='nav')
return;
this.nextimgidx=this.keeptrack()+(bool? 1 : -1)
this.nextimgidx=(this.nextimgidx<this.imgs.length)? this.nextimgidx : this.nextimgidx-this.imgs.length
this.nextimgidx=(this.nextimgidx>-1)? this.nextimgidx : this.imgs.length+this.nextimgidx
return;
}
if(this.fadeclear)
clearInterval(this.fadeclear);
if(bool){
var loadidx=this.nextimgidx+2
loadidx=(loadidx<this.imgs.length)? loadidx : loadidx-this.imgs.length;
loadidx=(loadidx>-1)? loadidx : this.imgs.length+loadidx;
this.loadimgidx[loadidx]=new Image();
this.loadimgidx[loadidx].src=this.imgs[loadidx][0];
}
else{
var loadidx=this.nextimgidx-2
loadidx=(loadidx<this.imgs.length)? loadidx : loadidx-this.imgs.length
loadidx=(loadidx>-1)? loadidx : this.imgs.length+loadidx
this.loadimgidx[loadidx]=new Image();
this.loadimgidx[loadidx].src=this.imgs[loadidx][0];
}
if(nav&&nav=='jump')
this.nextimgidx++;
else
this.nextimgidx=this.keeptrack()+(bool? 1 : -1)
this.nextimgidx=(this.nextimgidx<this.imgs.length)? this.nextimgidx : this.nextimgidx-this.imgs.length
this.nextimgidx=(this.nextimgidx>-1)? this.nextimgidx : this.imgs.length+this.nextimgidx
this.populateslide(this.tempobj, this.nextimgidx)
this.rotateimg()
}
}

inter_slide.prototype.go=function(id){
return this.da? this.da[id] : document.getElementById(id);
}

inter_slide.prototype.rotateimg=function(){
if(this.dom){
var cimg=this.go('theimg'+this.nextimgidx+'_'+this.issid)? this.go('theimg'+this.nextimgidx+'_'+this.issid) : null;
if(cimg&&typeof cimg.complete=='boolean'&&!cimg.complete){
var cacheobj=this
clearTimeout(this.loading)
this.loading=setTimeout(function(){cacheobj.rotateimg()}, 300)
return;
}
if (this.mouseovercheck==1){
var cacheobj=this
clearTimeout(this.mousep)
this.mousep=setTimeout(function(){cacheobj.rotateimg()}, 100)
return;
}
if(this.fade)
this.resetit()
var crossobj=this.tempobj=this.go(this.curcanvas)
crossobj.style.zIndex++
var othercanvas=this.curcanvas==this.canvasbase+"_0"? this.go(this.canvasbase+"_1") : this.go(this.canvasbase+"_0");
othercanvas.style.zIndex=0;
if(this.descriptions)
this.go('imgdsc'+this.issid).innerHTML = this.imgs[this.keeptrack()][1];
if(this.counter){
var padit='';
for (var p=0; p<this.cpad-(this.nextimgidx+1).toString().length; p++)
padit+='<span style="visibility:hidden;">0</span>';
this.go('thecnt'+this.issid).innerHTML = padit+(this.keeptrack()<this.imgs.length? this.keeptrack()+1 : 1);
}
if (this.jumpto)
this.go('goto'+this.issid).value=this.keeptrack()<this.imgs.length? this.keeptrack()+1 : 1;
this.jumperidx=this.keeptrack()<this.imgs.length? this.keeptrack()+1 : 1;
var fadeobj=this
clearInterval(this.fadeclear)
this.fadeclear=setInterval(function(){fadeobj.fadepic()},50)
this.curcanvas=(this.curcanvas==this.canvasbase+"_0")? this.canvasbase+"_1" : this.canvasbase+"_0"
}
else{
var v4imgobj=document.images['defaultslide'+this.issid]
v4imgobj.src=this.postimgs[this.nextimgidx].src
this.nextimgidx=(this.nextimgidx<this.imgs.length-1)? this.nextimgidx+1 : 0
}
}

inter_slide.prototype.resetit=function(){
this.degree=10
var crossobj=this.go(this.curcanvas)
if (crossobj.filters&&crossobj.filters[0]){
if (typeof crossobj.filters[0].opacity=="number") //if IE6+
crossobj.filters(0).opacity=this.degree
else //else if IE5.5-
crossobj.style.filter="alpha(opacity="+this.degree+")"
}
else if (crossobj.style.MozOpacity)
crossobj.style.MozOpacity=this.degree/101
else if (crossobj.style.KhtmlOpacity)
crossobj.style.KhtmlOpacity=this.degree/100
else if (crossobj.style.opacity&&!crossobj.filters)
crossobj.style.opacity=this.degree/101
}

inter_slide.prototype.startit=function(){
this.playing=1
var crossobj=this.go(this.curcanvas)
this.populateslide(crossobj, this.nextimgidx)
if (this.pausecheck==1){ //if slideshow should pause onmouseover
var cacheobj=this
var crossobjcontainer=this.go("master"+this.issid)
crossobjcontainer.onmouseover=function(){cacheobj.mouseovercheck=1}
crossobjcontainer.onmouseout=function(){cacheobj.mouseovercheck=0}
}
this.rotateimg()
if(this.no_auto)
this.gostop();
else if(this.man_start)
this.gostop(this.go('gostp'+this.issid));
else if(this.ics&&document.body.filters){ //kludge for IE5.5 bug
this.buttons(false);
}
}

inter_slide.prototype.gostop=function(el){
if(el)
el.value=el.value==' Stop '? ' Play ' : ' Stop ';
if(this.inprocess&&this.playing){
clearTimeout(this.inprocess);
this.nextimgidx-=this.faded;
}
this.playing=!this.playing;
if(this.playing){
clearInterval(this.fadeclear)
this.faded=1
this.changeimg(true);
}
else{
var loadidx=(this.keeptrack()<this.imgs.length-1)? this.keeptrack()+1 : 0
this.loadimgidx[loadidx]=new Image();
this.loadimgidx[loadidx].src=this.imgs[loadidx][0];
this.jumper(this.jumperidx? this.jumperidx : 0)
this.buttons(true);
}
}

inter_slide.prototype.keeptrack=function(){
if(!document.getElementsByTagName)
return this.nextimgidx;
var canvases=[this.go('canvas'+this.issid+'_0'), this.go('canvas'+this.issid+'_1')]
if(canvases[0].style.zIndex>canvases[1].style.zIndex&&canvases[0].getElementsByTagName("img")[0])
return parseInt(canvases[0].getElementsByTagName('img')[0].id.replace(/theimg/, ''))
else if(canvases[1].getElementsByTagName("img")[0])
return parseInt(canvases[1].getElementsByTagName('img')[0].id.replace(/theimg/, ''))
else
return this.nextimgidx;
}