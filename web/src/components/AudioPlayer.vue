<template>
  <div class="audio-player-container">
    <div class="player-controls">
      <a-button 
        type="primary" 
        shape="circle" 
        size="small" 
        @click="togglePlay"
        :loading="loading"
      >
        <a-icon :type="isPlaying ? 'pause' : 'caret-right'" />
      </a-button>
    </div>
    <div class="waveform-container" ref="waveform"></div>
  </div>
</template>

<script>
import WaveSurfer from 'wavesurfer.js';

export default {
  name: 'AudioPlayer',
  props: {
    audioUrl: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      wavesurfer: null,
      isPlaying: false,
      loading: true
    };
  },
  mounted() {
    this.$nextTick(() => {
      this.initWaveSurfer();
    });
  },
  beforeDestroy() {
    if (this.wavesurfer) {
      this.wavesurfer.destroy();
    }
  },
  watch: {
    audioUrl: {
      handler(newUrl) {
        if (this.wavesurfer && newUrl) {
          this.loading = true;
          this.loadAudio(newUrl);
        }
      },
      immediate: false
    }
  },
  methods: {
    initWaveSurfer() {
      // 创建wavesurfer实例，使用WebAudio后端
      this.wavesurfer = WaveSurfer.create({
        container: this.$refs.waveform,
        waveColor: '#ddd',
        progressColor: '#1890ff',
        cursorColor: 'transparent',
        barWidth: 2,
        barRadius: 2,
        barGap: 1,
        height: 40,
        responsive: true,
        normalize: true,
        backend: 'WebAudio' // 使用WebAudio后端而不是MediaElement
      });

      // 事件监听
      this.wavesurfer.on('ready', () => {
        this.loading = false;
      });

      this.wavesurfer.on('play', () => {
        this.isPlaying = true;
      });

      this.wavesurfer.on('pause', () => {
        this.isPlaying = false;
      });

      this.wavesurfer.on('finish', () => {
        this.isPlaying = false;
      });

      this.wavesurfer.on('error', (err) => {
        console.error('WaveSurfer error:', err);
        this.$message.error('音频加载失败');
        this.loading = false;
      });

      // 加载音频
      if (this.audioUrl) {
        this.loadAudio(this.audioUrl);
      }
    },
    loadAudio(url) {
      if (!url) return;
      
      this.wavesurfer.load(url);
    },
    togglePlay() {
      if (this.loading) return;
      
      if (this.wavesurfer) {
        this.wavesurfer.playPause();
      }
    }
  }
};
</script>

<style scoped>
.audio-player-container {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 5px;
}

.player-controls {
  margin-right: 10px;
}

.waveform-container {
  flex: 1;
  height: 40px;
}
</style>
