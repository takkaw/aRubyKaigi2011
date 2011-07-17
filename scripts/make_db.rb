#!/usr/bin/env ruby
# coding : utf-8

Fetch_from_network = true

require 'pathname'
BasePath = Pathname.new(__FILE__).parent.to_s 

require 'sequel'
class DB
  Path = BasePath + '/../assets/RubyKaigi2011.db'
  File.delete(Path) if Kernel.test(?e,Path)
  @@db = Sequel.sqlite(Path)
  unless @@db.table_exists? :RubyKaigi2011
    @@db.create_table :RubyKaigi2011 do
      primary_key :_id
      string :day
      string :room_en
      string :room_ja
      string :start
      string :end
      string :speaker_en
      string :speaker_ja
      string :title_en
      string :title_ja
      string :desc_en
      string :desc_ja
      string :lang
      string :speaker_bio_en
      string :speaker_bio_ja
      string :gravatar
    end 
  end
  unless @@db.table_exists? :android_metadata
    @@db.create_table :android_metadata do
      text :locale
    end
    @@db[:android_metadata] << {:locale => 'en_US'}
  end

  def self.write_db(data)
    @@db[:RubyKaigi2011] << data
  end

end

require 'open-uri'
require 'RMagick'
class Gravatar
  Size = 64
  Path = "#{BasePath}/../assets"
  BowPath = "#{Path}/bow_face.jpeg"
  def initialize
    open('http://rubykaigi.org/images/bow_face.png'){|bow|
      File.open(BowPath,'w') { |f|
        f.write bow.read
      }
    } if Fetch_from_network
    Magick::Image.read(BowPath).first.resize_to_fit(Size,Size).write(BowPath)
  end
  self.new
end

class Yaml_processor
  def self.presenters(yaml)
    speakers_en = []
    speakers_ja = []
    speakers_bio_en = ''
    speakers_bio_ja = ''
    gravatars = ''

    yaml.each { |pre|
      name_en = pre['name']['en']
      speakers_en << name_en

      name_ja = pre['name']['ja'] || name_en
      speakers_ja << name_ja

      bio_en = name_en + "\n"
      bio_en << "(#{pre['affiliation']['en']})\n" if pre['affiliation']['en']
      bio_en << "#{pre['bio']['en']}" if pre['bio']['en']
      speakers_bio_en << "\n\n" unless speakers_bio_en == ''
      speakers_bio_en << bio_en

      bio_ja = name_ja + "\n"
      bio_ja << "(#{(pre['affiliation']['ja'] || pre['affiliation']['en'])})\n" if (pre['affiliation']['ja'] || pre['affiliation']['en'])
      bio_ja << "#{(pre['bio']['ja'] || pre['bio']['en'])}" if (pre['bio']['ja'] || pre['bio']['en'])
      speakers_bio_ja << "\n\n" unless speakers_bio_ja == ''
      speakers_bio_ja << bio_ja

      speakers_bio_en.gsub! "#TODO" , ""
      speakers_bio_ja.gsub! "#TODO" , ""

      if gravatar = pre['gravatar']
        g_id = gravatar[0..7] # avoid too long file name
        open("http://www.gravatar.com/avatar/#{gravatar}?s=#{Gravatar::Size}") { |g|
          File.open("#{Gravatar::Path}/#{g_id}.jpeg",'w') { |f|
            f.write g.read
          }
        } if Fetch_from_network
      else
        g_id = 'bow_face'
      end

      unless gravatars == ''
        if Fetch_from_network
          img = Magick::ImageList.new(
            "#{Gravatar::Path}/#{gravatars}.jpeg",
            "#{Gravatar::Path}/#{g_id}.jpeg"
          )
          File.delete("#{Gravatar::Path}/#{gravatars}.jpeg") if gravatars != 'bow_face'
          gravatars << g_id
          img.append(false).write("#{Gravatar::Path}/#{gravatars}.jpeg")
        end
      else
        gravatars = g_id
      end
    }

    speakers_en = speakers_en.join(' , ')
    speakers_ja = speakers_ja.join(' , ')

    return [speakers_en,speakers_ja,speakers_bio_en,speakers_bio_ja,gravatars]

  end


  def self.event(ev,day,room_en,room_ja,start,_end,parent_title)
    title_en = ev['title']['en']
    title_ja = ev['title']['ja'] || title_en

    if parent_title
      title_format = "[#{parent_title}]\n%s"
      title_en = title_format % title_en
      title_ja = title_format % title_ja
    end

    presenters = ev['presenters']
    speakers_en,speakers_ja,speakers_bio_en,speakers_bio_ja,gravatars = 
      Yaml_processor::presenters(presenters) if presenters

    if abstract = ev['abstract']
      abstract_en = abstract['en']
      abstract_ja = abstract['ja'] || abstract_en
      abstract_en = abstract_ja if abstract_en == 'TBD'

      abstract_en.delete! "#TODO" if abstract_en
      abstract_ja.delete! "#TODO" if abstract_ja
    end 

    lang = ( ev['language'] || '' ).gsub('English','en').gsub('Japanese','ja')
    lang = "[#{lang}]" unless lang == ''

    common_event = ['Open','Break','Lunch','Transit time','Party at Ikebukuro (door open 19:30)'].include? title_en

    return if common_event && room_en == 'Sub Hall'

    data = { 
      :day => day,
      :room_en => common_event ? '' : room_en,
      :room_ja => common_event ? '' : room_ja,
      :start => start,
      :end => _end,
      :title_en => title_en,
      :title_ja => title_ja,
      :speaker_en => speakers_en,
      :speaker_ja => speakers_ja,
      :desc_en => abstract_en,
      :desc_ja => abstract_ja,
      :lang => lang,
      :speaker_bio_en => speakers_bio_en,
      :speaker_bio_ja => speakers_bio_ja,
      :gravatar => gravatars
    }   
    if sub_events = ev['sub_event_ids']
      sub_events.each { |sub|
        sub_yaml = "#{BasePath}/rubykaigi/db/2011/events/#{sub}.yaml"
        sub_ev = YAML.load_file(sub_yaml)
        self.event(sub_ev,day,room_en,room_ja,start,_end,title_en)
      }
    else
      DB.write_db(data)
    end

  end
end

require 'yaml'
require 'date'
Dir.glob(BasePath + '/rubykaigi/db/2011/room_timetables/*.yaml').sort!.each { |yaml|
  y = YAML.load_file(yaml)

  day = y['date'].day

  room_en = {'M' => 'Main Hall','S' => 'Sub Hall'}[y['room_id']]
  room_ja = {'M' => '大ホール','S' => '小ホール'}[y['room_id']] || room_en

  y['timeslots'].each { |slots|
    event = slots['event_ids']
    event.each { |ev|
      day = slots['start'].day
      start = "%02d:%02d" % [slots['start'].hour, slots['start'].min]
      _end = "%02d:%02d" % [slots['end'].hour,slots['end'].min]

      ev = YAML.load_file("#{BasePath}/rubykaigi/db/2011/events/#{ev}.yaml")

      Yaml_processor.event(ev,day,room_en,room_ja,start,_end,nil)

    } if event
  }
}

# add autograph events
require "#{BasePath}/autograph_event.rb"
AUTOGRAPH_EVENTS.each { |event|
  DB.write_db(event)
}

