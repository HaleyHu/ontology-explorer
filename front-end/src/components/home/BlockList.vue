<template>
  <div class="container">
    <div class="div-block-list form-group">
      <div class="row block-title-content">
        <div class="col-lg-6 block-title-wrapper">
          <p class="title font-blod">{{ $t('blockList.name') }}</p>
        </div>
        <div class="col-lg-6 block-title-wrapper">
          <p class="title-more float-right block-list-check-hand"  @click="toBlockListPage">{{ $t('all.more') }}</p>
        </div>
      </div>
      <div class="row">
            <div v-for="(block,index) in latestBlockList.info" class="col-lg-12 block-item-wrapper">
              <div class="col-lg-12 block-item-sub-wrapper">
                <div class=" block-item col-lg-6 text-left padding0 block-item-height font700 font-size18 click_able" @click="toBlockDetailPage(block.Height)" style="color:#32a4be">{{block.Height}}</div>
                <div v-if="block.TxnNum ==1" class="block-item col-lg-6 text-right padding0 font-size14">{{block.TxnNum}} Transaction</div>
                <div v-else class="block-item col-lg-6 text-right padding0 font-size14">{{block.TxnNum}}Transactions</div>
              </div>
              <div class="col-lg-12 block-item-sub-wrapper">
                <span class="block-item col-lg-6 text-left padding0 font-size14">{{block.BlockSize}} byte</span>
                <span v-if="getTime(block.BlockTime) < 60" class="block-item col-lg-6 text-right padding0 font-size14 ">{{showtime[index]}}s ago</span>
                <span v-else class="block-item col-lg-6 text-right padding0 font-size14 ">{{getShowDate(block.BlockTime)}} ago</span>
              </div>
            </div>
      </div>
    </div>
  </div>
</template>

<script>
  import {mapState} from 'vuex'
  import Helper from './../../helpers/helper.js'

  export default {
      name: "block-list",
    data() {
      return {
        info:[],
        blocktime:[0,0,0,0,0],
        timestamp : (new Date()).valueOf(),
        showtime:[0,0,0,0,0]
      }
    },
    created() {
      this.getBlockList()
      this.intervalBlock = setInterval(() => {
        this.getBlockList()
      }, 6000)
      this.intervalBlockstandard = setInterval(() => {
        for(var i =0;i<5;i++){
          var time = this.showtime[i] + 1
          this.$set(this.showtime,i,time)
        }
      }, 1000)
    },
    watch: {
      '$route': 'getBlockList',
      'latestBlockList.info':function(){
        for(var i =0;i<5;i++){
          this.showtime[i] = this.getTime(this.latestBlockList.info[i].BlockTime)
        }
      },
    },
    computed: {
      ...mapState({
        latestBlockList: state => state.BlockList.LatestBlockList
      }),
    },
    methods: {
      getBlockList() {
        // do something
        
        this.$store.dispatch('getBlockList').then(response => {
          /* console.log("no1",response) */
         /*  this.getNewTime() */
        }).catch(error => {
          console.log(error)
        })
      },
      toBlockListPage(){
        this.$router.push({ name:'blockListDetail', params:{pageSize:10,pageNumber:1}})
      },
      toBlockDetailPage($blockHeight){
        this.$router.push({ name:'blockDetail', params:{param:$blockHeight}})
      },
      getTime($time){
        return Helper.getDateTime($time)
      },
      getDate($time){
        return Helper.getDate($time)
      },
      getShowDate($time){
        var time = this.getTime($time)
        return Helper.getshowDate(time)
      },
      countDownTime:function(){
        
          for(var i=0;i<this.info.length;i++){
            this.info[i].showtime = this.info[i].showtime+1
          }
      },
      getNewTime:function(){
        
          var i=0
          var timestamp = (new Date()).valueOf();
          for(i=0;i<this.latestBlockList.info.length;i++){
            /* this.latestBlockList.info[i].showtime = (timestamp - this.latestBlockList.info[i].BlockTime*1000)/1000 */
            this.latestBlockList.info[i].showtime = timestamp
          }
      },
    },
    beforeDestroy () {
      clearInterval(this.intervalBlock)
      clearInterval(this.intervalBlockstandard)
    },
  }
</script>

<style scoped>

</style>
